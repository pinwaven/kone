package poct.device.app.bean

data class WorkFlowV2(
    val actions: ArrayList<WorkFlowActionV2>,
) {
    private var index: Int = -1

    fun current(): WorkFlowActionV2? {
        return if (actions.isNotEmpty() && index >= 0) actions[index] else null
    }

    fun next(onNext: (WorkFlowActionV2) -> Unit = { }) {
        index++
        try {
            if (index >= actions.size) {
                throw IndexOutOfBoundsException()
            }
            onNext(actions[index])
        } catch (e: Exception) {
            index--
        }
    }

    companion object {
        val EMPTY = WorkFlowV2(ArrayList())
    }
}

