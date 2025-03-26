package com.azheng.androidutils

/**
 * @date 2025/3/26.
 * description：
 */
class PathUtils {
    companion object{

        /**
         * 获取内部文件目录路径
         * 路径示例：/data/data/包名/files
         */
        fun getInternalFilesPath(): String = Utils.getApplication().filesDir.absolutePath

        /**
         * 获取外部文件目录路径
         * 路径示例：/storage/emulated/0/Android/data/包名/files
         * @param type 子目录类型（如 Environment.DIRECTORY_DOWNLOADS）
         *             默认null返回主目录
         */
        fun getExternalFilesPath(type: String? = null): String? {
            return Utils.getApplication().getExternalFilesDir(type)?.absolutePath
        }

        /**
         * 获取外部缓存目录路径
         * 路径示例：/storage/emulated/0/Android/data/包名/cache
         */
        fun getExternalCachePath(): String? {
            return Utils.getApplication().externalCacheDir?.absolutePath
        }
    }
}