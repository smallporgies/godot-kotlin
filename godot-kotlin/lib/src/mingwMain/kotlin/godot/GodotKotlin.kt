package godot

import godotapi.*
import kotlinx.cinterop.*

typealias String = godot_string
typealias Variant = godot_variant
typealias Array = godot_array
typealias NodePath = godot_node_path
typealias PoolStringArray = godot_pool_string_array

class KGDNative(
        val c: godot_gdnative_core_api_struct,
        private val options: godot_gdnative_init_options
) {
    fun print(value: Any?) {
        memScoped {
            val string = (value?.toString() ?: "null")
            val stringPointer = alloc<godot_string>().ptr
            c.godot_string_new!!(stringPointer)
            c.godot_string_parse_utf8_with_len!!(stringPointer, string.cstr.ptr, string.length)
            c.godot_print!!(stringPointer)
            c.godot_string_destroy!!(stringPointer)
        }
    }

    fun printAPIVersion() {
        print("GDNative API version: " + c.version.major + "." + c.version.minor)
    }
}

class KNativeScript(
        val c: godot_gdnative_ext_nativescript_api_struct,
        val handle: COpaquePointer
) {
    fun printAPIVersion() {
        api.print("NativeScript API version: " + c.version.major + "." + c.version.minor)
    }
}

val api: KGDNative
    get() = gdNative ?: throw IllegalStateException("Attempted to access GDNative but it was not initialized")

var nativeScript: KNativeScript? = null
private var gdNative: KGDNative? = null

fun gdNativeInit(options: godot_gdnative_init_options) {
    gdNative = KGDNative(options.api_struct!![0], options)
    api.print("Initializing Kotlin library. In editor: ${options.in_editor}")
}

fun gdNativeTerminate(options: godot_gdnative_terminate_options) {
    api.print("De-initializing Kotlin library. In editor: ${options.in_editor}")
    gdNative = null
    nativeScript = null
}

@ExperimentalUnsignedTypes
fun nativeScriptInit(handle: COpaquePointer) {
    api.print("Initializing Kotlin-Godot nativescript.")

    for (i in 0..api.c.num_extensions.toInt()) {
        val extension = api.c.extensions!![i]!!
        if (extension[0].type == GDNATIVE_API_TYPES.GDNATIVE_EXT_NATIVESCRIPT.value) {
            nativeScript =
                    KNativeScript(extension.reinterpret<godot_gdnative_ext_nativescript_api_struct>()[0], handle)
            break
        }
    }

    api.printAPIVersion()
    nativeScript?.printAPIVersion()
}