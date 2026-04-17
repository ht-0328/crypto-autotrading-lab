package cryptoautotrading.domain.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * データ出力関連の設定
 *
 * @property outputPath 取引履歴を出力するファイルのパス
 * @property statePath アプリケーションの状態を保存するファイルのパス
 */
data class OutputConfig(
    @JsonProperty("output_path")
    val outputPath: String,
    @JsonProperty("state_path")
    val statePath: String
)
