package poct.device.app.entity.service

import poct.device.app.App
import poct.device.app.entity.CardConfig
import java.time.LocalDateTime
import java.util.Collections

object CardConfigService {
    suspend fun findAll(): List<CardConfig> {
        return App.getDatabase().cardConfigDao().findAll() ?: Collections.emptyList()
    }

    suspend fun findById(id: String): CardConfig? {
        return App.getDatabase().cardConfigDao().findById(id)
    }

    suspend fun findByIden(type: String, code: String): CardConfig? {
        return App.getDatabase().cardConfigDao().findByIden(type, code)
    }

    suspend fun add(entity: CardConfig) {
        entity.gmtCreated = LocalDateTime.now()
        entity.gmtModified = LocalDateTime.now()
        return App.getDatabase().cardConfigDao().add(entity)
    }

    suspend fun update(entity: CardConfig) {
        entity.gmtModified = LocalDateTime.now()
        return App.getDatabase().cardConfigDao().update(entity)
    }

    suspend fun delete(entity: CardConfig) {
        return App.getDatabase().cardConfigDao().delete(entity)
    }
}