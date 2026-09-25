package com.ai.assistance.operit.api.voice

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

/** 语音服务接口，定义与不同语音引擎进行交互的标准方法 */
interface VoiceService {
    /** 当前语音引擎是否初始化完成 */
    val isInitialized: Boolean

    /** 当前语音引擎是否正在播放 */
    val isSpeaking: Boolean

    /** 语音状态Flow，用于UI观察状态变化 */
    val speakingStateFlow: Flow<Boolean>

    /**
     * 初始化语音引擎
     *
     * @return 初始化是否成功
     */
    suspend fun initialize(): Boolean

    /**
     * 文本转语音，将文本通过TTS引擎转换为语音并播放
     *
     * @param text 要转换为语音的文本
     * @param interrupt 是否中断当前正在播放的语音，默认为true
     * @param rate 语速覆盖值（可选）。为 null 时使用设置中的全局语速。
     * @param pitch 音调覆盖值（可选）。为 null 时使用设置中的全局音调。
     * @param extraParams 额外的请求参数，用于传递特定于实现的参数
     * @return 操作是否成功
     */
    suspend fun speak(
            text: String,
            interrupt: Boolean = true,
            rate: Float? = null,
            pitch: Float? = null,
            extraParams: Map<String, String> = emptyMap()
    ): Boolean

    /**
     * 停止当前正在播放的语音
     *
     * @return 操作是否成功
     */
    suspend fun stop(): Boolean

    /**
     * 暂停当前正在播放的语音
     *
     * @return 操作是否成功
     */
    suspend fun pause(): Boolean

    /**
     * 继续播放暂停的语音
     *
     * @return 操作是否成功
     */
    suspend fun resume(): Boolean

    /** 释放语音引擎资源 */
    fun shutdown()

    /**
     * 获取当前可用的语音列表
     *
     * @return 语音列表，包含语音ID和语音名称
     */
    suspend fun getAvailableVoices(): List<Voice>

    /**
     * 设置当前使用的语音
     *
     * @param voiceId 语音ID
     * @return 设置是否成功
     */
    suspend fun setVoice(voiceId: String): Boolean

    /**
     * 将文本加入语音队列后立即返回，不等待该段播放结束。
     *
     * 支持流水的实现（如 HttpVoiceProvider）会让「合成下一段」与「播放当前段」重叠，
     * 从而消除片段之间的一次完整网络往返还等待；默认实现退化为 [speak] 的同步语义，
     * 不支持流水的提供方行为与之前完全一致。
     *
     * @return 一个在该段播放结束时完成的 Deferred，语义与 [speak] 的返回值一致
     */
    suspend fun enqueueSpeak(
            text: String,
            interrupt: Boolean = false,
            rate: Float? = null,
            pitch: Float? = null,
            extraParams: Map<String, String> = emptyMap()
    ): Deferred<Boolean> {
        val result = CompletableDeferred<Boolean>()
        result.complete(speak(text, interrupt, rate, pitch, extraParams))
        return result
    }

    /**
     * 表示TTS语音的数据类
     *
     * @property id 语音唯一标识符
     * @property name 语音名称
     * @property locale 语音对应的语言和地区
     * @property gender 语音性别，可能为MALE、FEMALE或NEUTRAL
     */
    data class Voice(val id: String, val name: String, val locale: String, val gender: String)
}
