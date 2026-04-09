package com.example.remote_tv.data.model

/**
 * Đại diện cho một macro — chuỗi lệnh TV được gộp thành một hành động duy nhất.
 *
 * @param id         ID duy nhất, dùng để lưu/tải từ DataStore.
 * @param name       Tên hiển thị người dùng đặt (vd: "Open Netflix", "Sleep Timer").
 * @param commands   Danh sách các lệnh TV theo thứ tự (vd: ["KEY_HOME", "TEXT:Netflix", "KEY_ENTER"]).
 * @param delayMs    Thời gian chờ giữa các lệnh (ms), mặc định 350ms.
 * @param iconName   Tên icon Material tùy chọn để hiển thị trên UI.
 */
data class Macro(
    val id: String,
    val name: String,
    val commands: List<String>,
    val delayMs: Long = 350L,
    val iconName: String = "PlayCircle",
)
