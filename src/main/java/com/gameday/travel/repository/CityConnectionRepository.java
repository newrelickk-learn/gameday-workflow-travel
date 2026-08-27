package com.gameday.travel.repository;

import com.gameday.travel.entity.CityConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CityConnectionRepository extends JpaRepository<CityConnection, Long> {

    // 直行便があるかどうかの単純な等値検索（from_city_idのインデックスを使うため常に高速）。
    // 北九州の各ハブは1日2便あるため同じ(from,to)ペアが複数行になり得るので、
    // MIN()で最安の1便に絞る（0件ならNULL=Optional.emptyになる）。
    @Query(value = "SELECT MIN(distance_units) FROM city_connections WHERE from_city_id = :fromCityId AND to_city_id = :toCityId",
            nativeQuery = true)
    Optional<Integer> findDirectDistance(@Param("fromCityId") Long fromCityId, @Param("toCityId") Long toCityId);

    // 直行便が無い場合のフォールバック: 経由地点を辿って最安ルートを探す再帰CTE。
    // 訪問済み都市を除外しながら最大7ホップまで総当たりで経路を展開するため、
    // 分岐の多いグラフ（＝北九州のように直行便が無く、複数のハブ都市経由になるケース）では
    // 展開される行数が組み合わせ的に増え、実行コストが跳ね上がる。
    // 主要都市どうしは直行便テーブル（findDirectDistance）だけで解決するため、
    // このクエリは北九州が絡む一部の組み合わせでしか実行されない。
    // ホップ数（7）と北九州の各ハブへの便数（DBシード側、現行8便＝現行2便+廃止路線6便）を
    // 組み合わせ、実測（本番相当DBでEXPLAIN ANALYZE）で5秒前後になるよう調整済み。
    // New Relic Javaエージェントのslow SQL/Explain Plan収集の既定閾値（500ms）だけでなく、
    // NRDOT(nrpostgresqlreceiver)側のクエリサンプリング間隔（15秒）に対しても、実行中に
    // サンプルされる確率を上げるため十分な長さ（1秒未満だと拾われないことがある）を確保する。
    //
    // hop_countの初期値・加算をGREATEST(1 :: integer, 1 :: integer)にしているのは意図的。
    // pg_stat_statementsはリテラルをパラメータ化する際、単純な値への型キャスト（1::integer）は
    // パーサーの定数畳み込みで消えてしまい正規化後のクエリに反映されない。一方、GREATESTのような
    // オーバーロードされた関数の引数として型キャストすると、関数解決の結果としてASTに残るため、
    // nrpostgresqlreceiver（EXPLAIN実行時にNULLパラメータを渡す）が型不明(unknown)による
    // "operator does not exist"エラーを起こさずにEXPLAINを取得できるようになる。
    //
    // "::"の前後に空白を入れているのも意図的（詰めた"::"にしない）。New RelicのAPM⇔DB
    // クロスエンジン相関ハッシュ(db.query.text.normalized.hash)の計算アルゴリズムは、
    // ":"の直後に識別子が続くとOracle/PL-SQL形式のbind変数(:name)と誤認識してプレースホルダに
    // 潰してしまう。詰めた"?::integer"だと2文字目の":"が":integer"という独立したプレースホルダに
    // 誤変換され、pg_stat_statementsが出力する"? :: integer"（コロン前後に空白を入れて整形する）
    // 側とは異なる正規化結果・ハッシュになってしまう。空白を入れておけば誤認識されず、
    // APM側(db.statement、Javaのソースコードそのまま)とInfrastructure側(db.query.text、
    // PostgreSQLが整形した後のテキスト)の正規化ハッシュが一致する。

    @Query(value = """
            WITH RECURSIVE route_search AS (
                SELECT
                    cc.to_city_id AS current_city_id,
                    ARRAY[cc.from_city_id, cc.to_city_id] AS visited,
                    cc.distance_units AS total_distance,
                    GREATEST(1 :: integer, 1 :: integer) AS hop_count
                FROM city_connections cc
                WHERE cc.from_city_id = :fromCityId

                UNION ALL

                SELECT
                    cc.to_city_id,
                    rs.visited || cc.to_city_id,
                    rs.total_distance + cc.distance_units,
                    rs.hop_count + GREATEST(1 :: integer, 1 :: integer)
                FROM route_search rs
                JOIN city_connections cc ON cc.from_city_id = rs.current_city_id
                WHERE rs.hop_count < 7
                  AND NOT (cc.to_city_id = ANY (rs.visited))
            )
            SELECT MIN(total_distance)
            FROM route_search
            WHERE current_city_id = :toCityId
            """, nativeQuery = true)
    Optional<Integer> findCheapestDistanceViaLayovers(@Param("fromCityId") Long fromCityId, @Param("toCityId") Long toCityId);
}
