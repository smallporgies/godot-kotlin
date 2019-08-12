package godot

import godot.internal.godot_variant
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.invoke
import kotlin.reflect.KClass

@UseExperimental(ExperimentalUnsignedTypes::class)
class Variant : CoreType<godot_variant> {
    override val _wrapped: CPointer<godot_variant>

    internal constructor(_wrapped: CPointer<godot_variant>) {
        this._wrapped = _wrapped
    }

    internal constructor() : this(godot.alloc(godot_variant.size))

    constructor(value: Float) : this() {
        godot.api.godot_variant_new_real!!(_wrapped, value.toDouble())
    }

    constructor(value: String) : this() {
        godot.api.godot_variant_new_string!!(_wrapped, value.toGodotString())
    }

    constructor(value: Vector2) : this() {
        godot.api.godot_variant_new_vector2!!(_wrapped, value._wrapped)
    }

    constructor(value: Int) : this() {
        godot.api.godot_variant_new_int!!(_wrapped, value.toLong())
    }

    constructor(value: UInt) : this() {
        godot.api.godot_variant_new_uint!!(_wrapped, value.toULong())
    }

    fun asFloat(): Float {
        return godot.api.godot_variant_as_real!!(_wrapped).toFloat()
    }

    fun asString(): String {
        return godot.api.godot_variant_as_string!!(_wrapped).toKotlinString()
    }

    fun asVector2(): Vector2 {
        return Vector2(godot.api.godot_variant_as_vector2!!(_wrapped))
    }

    fun asInt(): Int {
        return godot.api.godot_variant_as_int!!(_wrapped).toInt()
    }

    fun asUInt(): UInt {
        return godot.api.godot_variant_as_uint!!(_wrapped).toUInt()
    }

    fun <T : Any> cast(type: KClass<T>): T {
        return when (type) {
            Float::class -> this.asFloat()
            String::class -> this.asString()
            Vector2::class -> this.asVector2()
            Int::class -> this.asInt()
            UInt::class -> this.asUInt()
            else -> throw UnsupportedOperationException("Could not cast Variant to $type")
        } as T
    }

    companion object {
        internal fun from(value: Any?): Variant? {
            return when (value) {
                is Float -> Variant(value)
                is Double -> Variant(value.toFloat())
                is Int -> Variant(value)
                is Long -> Variant(value.toInt())
                is UInt -> Variant(value)
                is ULong -> Variant(value.toUInt())
                is String -> Variant(value)
                is Vector2 -> Variant(value)
                is Unit -> null
                else -> nil()
            }
        }

        fun nil(): Variant {
            val variant = Variant()
            godot.api.godot_variant_new_nil!!(variant._wrapped)
            return variant
        }
    }
}