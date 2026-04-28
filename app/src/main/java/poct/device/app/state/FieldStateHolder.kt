package poct.device.app.state

/**
 * *
 * @date ：2024/4/14
 * @desc：字段状态集合
 */
class FieldStateHolder(
    private val stateList: ArrayList<FieldState> = ArrayList(),
) {

    /**
     * 添加状态，并返回一个新的对象。 如果已存在相同name的状态，则替换掉原来的
     */
    fun getStateList(): ArrayList<FieldState>{
        return stateList
    }

    /**
     * 添加状态，并返回一个新的对象。 如果已存在相同name的状态，则替换掉原来的
     */
    fun put(fieldState: FieldState) {
        doRemove(fieldState.name)
        stateList.add(fieldState)
    }

    /**
     * 移除状态，并返回一个新的对象。
     */
    fun remove(fieldState: FieldState) {
        doRemove(fieldState.name)
    }

    fun remove(name: String) {
        doRemove(name)
    }

    fun get(name: String): FieldState? {
        return stateList.filter { it.name == name }.getOrNull(0)
    }

    /**
     * 克隆1个新对象
     */
    fun clone() : FieldStateHolder {
        return FieldStateHolder(ArrayList(stateList))
    }

    fun hasErrors(): Boolean {
        return stateList.any { it.state == FieldState.STATE_ERROR }
    }

    private fun doRemove(name: String): FieldState? {
        val fieldState = get(name)
        fieldState?.apply { stateList.remove(this) }
        return fieldState
    }


}
