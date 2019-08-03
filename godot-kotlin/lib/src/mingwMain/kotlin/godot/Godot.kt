package godot

import godotapi.*
import kotlinx.cinterop.*
import kotlin.reflect.KFunction2

const val GDNATIVE_INIT = "godot_gdnative_init"
const val GDNATIVE_TERMINATE = "godot_gdnative_terminate"
const val NATIVESCRIPT_INIT = "godot_nativescript_init"

typealias GDNativeInitOptions = godot_gdnative_init_options
typealias GDNativeTerminateOptions = godot_gdnative_terminate_options
typealias NativescriptHandle = COpaquePointer

typealias Array = godot_array
typealias Basis = godot_basis
typealias Color = godot_color
typealias Dictionary = godot_dictionary
typealias Error = godot_error
typealias NodePath = godot_node_path
typealias Plane = godot_plane
typealias PoolByteArray = godot_pool_byte_array
typealias PoolIntArray = godot_pool_int_array
typealias PoolRealArray = godot_pool_real_array
typealias PoolStringArray = godot_pool_string_array
typealias PoolVector2Array = godot_pool_vector2_array
typealias PoolVector3Array = godot_pool_vector3_array
typealias PoolColorArray = godot_pool_color_array
typealias Quat = godot_quat
typealias Rect2 = godot_rect2
typealias AABB = godot_aabb
typealias RID = godot_rid
typealias GString = godot_string
typealias Transform = godot_transform
typealias Transform2D = godot_transform2d
typealias Variant = godot_variant
typealias Vector2 = godot_vector2
typealias Vector3 = godot_vector3

typealias VariantType = godot_variant_type
typealias VariantOperator = godot_variant_operator
typealias Vector3Axis = godot_vector3_axis

class Godot {

    val tagDB = TagDB()
    lateinit var api: godot_gdnative_core_api_struct
    lateinit var gdnlib: COpaquePointer
    lateinit var nativescriptApi: godot_gdnative_ext_nativescript_api_struct
    lateinit var nativescript11Api: godot_gdnative_ext_nativescript_1_1_api_struct

    fun print(message: Any?) {
        memScoped {
            val string = message.toString()
            val data: CPointer<godot_string> = api.godot_alloc!!(string.length)!!.reinterpret()
            api.godot_string_new!!(data)
            api.godot_string_parse_utf8!!(data, string.cstr.ptr)
            api.godot_print!!(data)
            api.godot_string_destroy!!(data)
        }
    }

    @UseExperimental(ExperimentalUnsignedTypes::class)
    fun gdNativeInit(options: GDNativeInitOptions) {
        api = if (options.api_struct?.pointed != null) options.api_struct?.pointed!! else {
            println("api is null!")
            throw NullPointerException("api is null!")
        }
        gdnlib = if (options.gd_native_library != null) options.gd_native_library!! else {
            println("gdnlib is null!")
            throw NullPointerException("gdnlib is null!")
        }

        for (i in 0 until api.num_extensions.toInt()) {
            when (api.extensions?.get(i)?.pointed?.type) {
                GDNATIVE_API_TYPES.GDNATIVE_EXT_NATIVESCRIPT.value -> {
                    val temp = api.extensions?.get(i)?.reinterpret<godot_gdnative_ext_nativescript_api_struct>()?.pointed
                    nativescriptApi = if (temp != null) temp else {
                        println("nativescriptApi is null!")
                        throw NullPointerException("nativescriptApi is null!")
                    }
                    var extension: CPointer<godot_gdnative_api_struct>? = nativescriptApi.next
                    while (extension != null) {
                        if (extension.pointed.version.major == 1u && extension.pointed.version.minor == 1u) {
                            nativescript11Api = extension.reinterpret<godot_gdnative_ext_nativescript_1_1_api_struct>().pointed
                        }
                        extension = extension.pointed.next
                    }
                }
            }
        }
    }

    fun gdNativeTerminate(options: GDNativeTerminateOptions) = Unit

    @UseExperimental(ExperimentalUnsignedTypes::class)
    fun gdnativeProfilingAddData(signature: String, time: ULong) {
        memScoped {
            if (nativescript11Api.godot_nativescript_profiling_add_data != null) {
                nativescript11Api.godot_nativescript_profiling_add_data!!(signature.cstr.ptr, time)
            } else {
                println("nativescript11Api.godot_nativescript_profiling_add_data is null!")
                throw NullPointerException("nativescript11Api.godot_nativescript_profiling_add_data is null!")
            }
        }
    }

    fun nativescriptTerminate(handle: NativescriptHandle) {
        if (nativescript11Api.godot_nativescript_unregister_instance_binding_data_functions != null) {
            nativescript11Api.godot_nativescript_unregister_instance_binding_data_functions!!(languageIndex)
        } else {
            println("nativescript11Api.godot_nativescript_unregister_instance_binding_data_functions is null!")
            throw NullPointerException("nativescript11Api.godot_nativescript_unregister_instance_binding_data_functions is null!")
        }
    }

    fun nativeScriptInit(handle: NativescriptHandle) {
        nativescriptHandle = handle

        memScoped {
            val binding_funcs = cValue<godot_instance_binding_functions>() // TODO
            languageIndex = nativescript11Api.godot_nativescript_register_instance_binding_data_functions!!(binding_funcs)

//            _registerTypes()
//            _initMethodBindings()
        }
    }

    lateinit var nativescriptHandle: COpaquePointer
    var languageIndex: Int = -1

    inline fun <reified T : S, reified S : Any> registerClass(registerMethods: () -> Unit = {}) {
        memScoped {
            val create = cValue<godot_instance_create_func>() // TODO
            val destroy = cValue<godot_instance_destroy_func>() // TODO

            val typeTag = T::class.hashCode()
            val baseTypeTag = S::class.hashCode()

            val typeName = T::class.qualifiedName?.substringAfter("godot.") ?: typeTag.toString()
            val baseTypeName = S::class.qualifiedName?.substringAfter("godot.") ?: baseTypeTag.toString()

            print("registering class $typeName : $baseTypeName, with tag $typeTag : $baseTypeTag")
            tagDB.registerType(typeTag, baseTypeTag)

            nativescriptApi.godot_nativescript_register_class!!(nativescriptHandle, typeName.cstr.ptr, baseTypeName.cstr.ptr, create, destroy)
            nativescript11Api.godot_nativescript_set_type_tag!!(nativescriptHandle, typeName.cstr.ptr, alloc<IntVar> { value = typeTag }.ptr)
            registerMethods()
        }
    }

    @UseExperimental(ExperimentalUnsignedTypes::class)
    inline fun <reified T : Any> registerMethod(function: KFunction2<T, Float, Unit>, rpcType: UInt = GODOT_METHOD_RPC_MODE_DISABLED) {
        memScoped {
            val methodName = function.name.cstr.ptr
            val className = (T::class.qualifiedName?.substringAfter("godot.")
                    ?: T::class.hashCode().toString()).cstr.ptr
            val method = cValue<godot_instance_method>() // TODO
            val attr = cValue<godot_method_attributes> {
                rpc_type = rpc_type
            }

            nativescriptApi.godot_nativescript_register_method!!(nativescriptHandle, className, methodName, attr, method)
        }
    }
}

val godot = Godot()
