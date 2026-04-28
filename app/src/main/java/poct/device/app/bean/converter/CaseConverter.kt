package poct.device.app.bean.converter

import poct.device.app.bean.CaseBean
import poct.device.app.bean.card.Card
import poct.device.app.bean.card.CardBatch
import poct.device.app.bean.card.CardInfoBean
import poct.device.app.entity.Case
import poct.device.app.utils.app.AppLocalDateUtils
import java.time.LocalDateTime

object CaseConverter {
    fun toEntity(bean: CaseBean): Case {
        return Case(
            id = bean.id,
            name = bean.name,
            gender = bean.gender,
            birthday = AppLocalDateUtils.parseDate(bean.birthday),
            qrCode = bean.qrCode,
            caseId = bean.caseId,
            caseType = bean.caseType,
            reagentId = bean.reagentId,
            time = AppLocalDateUtils.parseDateTime(bean.workTime),
            type = bean.type,
            state = bean.state,
            result = bean.workResult,
            points = bean.workPoints,
            gmtCreated = LocalDateTime.now(),
            gmtModified = LocalDateTime.now(),
        )
    }

    fun fillEntity(bean: CaseBean, entity: Case): Case {
        return entity.copy(
            id = bean.id,
            name = bean.name,
            gender = bean.gender,
            birthday = AppLocalDateUtils.parseDate(bean.birthday),
            qrCode = bean.qrCode,
            caseId = bean.caseId,
            caseType = bean.caseType,
            reagentId = bean.reagentId,
            time = AppLocalDateUtils.parseDateTime(bean.workTime),
            type = bean.type,
            state = bean.state,
            result = bean.workResult,
            points = bean.workPoints,
        )
    }

    fun fromEntity(entity: Case): CaseBean {
        return CaseBean(
            id = entity.id,
            name = entity.name,
            gender = entity.gender,
            birthday = AppLocalDateUtils.formatDate(entity.birthday),
            caseId = entity.caseId,
            caseType = entity.caseType,
            qrCode = entity.qrCode,
            reagentId = entity.reagentId,
            type = entity.type,
            workTime = AppLocalDateUtils.formatDateTime(entity.time),
            workResult = entity.result,
            state = entity.state,
            workPoints = entity.points,
        )
    }

    fun fillCardInfoBean(caseBean: CaseBean): CardInfoBean {
        return CardInfoBean(
            card = Card(
                code = caseBean.qrCode,
            ),
            cardBatch = CardBatch(
                type = caseBean.type,
                code = caseBean.reagentId,
            ),
        )
    }
}