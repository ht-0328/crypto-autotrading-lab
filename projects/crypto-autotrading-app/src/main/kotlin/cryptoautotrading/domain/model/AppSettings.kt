package cryptoautotrading.domain.model

/**
 * アプリケーションの基本設定
 *
 * @property interval データを取得する間隔
 * @property phase 現在の開発フェーズ。実注文が許可されるのは Phase3 以降で、
 *   それ未満のフェーズでは設定値にかかわらず実注文経路に入らない。未指定時は安全側の 1 とする。
 */
data class AppSettings(
    val interval: String,
    val phase: Int = 1
)
