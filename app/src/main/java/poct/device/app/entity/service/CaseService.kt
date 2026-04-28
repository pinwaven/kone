package poct.device.app.entity.service

import androidx.sqlite.db.SimpleSQLiteQuery
import poct.device.app.App
import poct.device.app.bean.CaseQueryBean
import poct.device.app.entity.Case
import poct.device.app.utils.app.AppLocalDateUtils
import java.time.LocalDateTime

object CaseService {
    suspend fun query(query: CaseQueryBean): List<Case> {
        val sqlBuilder = StringBuilder("SELECT * from tbl_case")
        val argList = ArrayList<Any>()
        var sep = " WHERE "
        if (query.name.isNotEmpty()) {
            sqlBuilder.append(sep).append("name like :name")
            argList.add("%${query.name}%")
            sep = " AND "
        }
        if (query.caseType.isNotEmpty()) {
            sqlBuilder.append(sep).append("type=:type")
            argList.add(query.caseType)
            sep = " AND "
        }
        if (query.dateStarted.isNotEmpty()) {
            sqlBuilder.append(sep).append("time>=:dateStarted")
            argList.add(AppLocalDateUtils.timestampFromDate(AppLocalDateUtils.parseDate(query.dateStarted)))
            sep = " AND "
        }
        if (query.dateEnded.isNotEmpty()) {
            sqlBuilder.append(sep).append("time<:dateEnded")
            argList.add(
                AppLocalDateUtils.timestampFromDate(
                    AppLocalDateUtils.parseDate(query.dateEnded).plusDays(1)
                )
            )
        }
        sqlBuilder.append(" ORDER BY time DESC")

        val sql = SimpleSQLiteQuery(sqlBuilder.toString(), argList.toArray())
        return App.getDatabase().caseDao().query(sql)
    }

    suspend fun findById(id: String): Case? {
        return App.getDatabase().caseDao().findById(id)
    }

    suspend fun findByCardCode(qrCode: String): Case? {
        return App.getDatabase().caseDao().findByQrCode(qrCode)
    }

    suspend fun add(entity: Case) {
        entity.gmtCreated = LocalDateTime.now()
        return App.getDatabase().caseDao().add(entity)
    }

    suspend fun update(entity: Case) {
        entity.gmtModified = LocalDateTime.now()
        return App.getDatabase().caseDao().update(entity)
    }

    suspend fun delete(entity: Case) {
        return App.getDatabase().caseDao().delete(entity)
    }
}