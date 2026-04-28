package poct.device.app.bean

data class WorkFlow(
    val actions: ArrayList<WorkFlowAction>,
) {

    private var index: Int = -1

    fun current(): WorkFlowAction? {
        return if (actions.size > 0 && index >= 0) actions[index] else null
    }

    fun next(onNext: (WorkFlowAction) -> Unit = { }) {
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
        val EMPTY = WorkFlow(ArrayList())
    }
}

