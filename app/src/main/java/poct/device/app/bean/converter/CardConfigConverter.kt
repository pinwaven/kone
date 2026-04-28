package poct.device.app.bean.converter

import android.annotation.SuppressLint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.lang.math.NumberUtils
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CardTopBean
import poct.device.app.bean.CardVarBean
import poct.device.app.entity.CardConfig
import poct.device.app.entity.CardTop
import poct.device.app.entity.CardVar
import java.time.LocalDateTime

object CardConfigConverter {
    fun toEntity(bean: CardConfigBean): CardConfig {
        return CardConfig(
            id = bean.id,
            name = bean.name,
            code = bean.code,
            type = bean.type,
            prodDate = bean.prodDate,
            endDate = bean.endDate,
            scanStart = NumberUtils.toDouble(bean.scanStart),
            scanEnd = NumberUtils.toDouble(bean.scanEnd),
            scanPPMM = NumberUtils.toInt(bean.scanPPMM),
            topList = Json.encodeToString(toTopEntityList(bean.topList)),
            varList = Json.encodeToString(toVarEntityList(bean.varList)),
            gmtCreated = LocalDateTime.now(),
            gmtModified = LocalDateTime.now(),
            ft0 = NumberUtils.toInt(bean.ft0),
            xt1 = NumberUtils.toInt(bean.xt1),
            ft1 = NumberUtils.toInt(bean.ft1),
            typeScore = NumberUtils.toDouble(bean.typeScore),
            scope = NumberUtils.toDouble(bean.scope),
            cAvg = NumberUtils.toDouble(bean.cAvg),
            cStd = NumberUtils.toDouble(bean.cStd),
            cMin = NumberUtils.toDouble(bean.cMin),
            cMax = NumberUtils.toDouble(bean.cMax),
            cutOff1 = NumberUtils.toDouble(bean.cutOff1),
            cutOff2 = NumberUtils.toDouble(bean.cutOff2),
            cutOff3 = NumberUtils.toDouble(bean.cutOff3),
            cutOff4 = NumberUtils.toDouble(bean.cutOff4),
            cutOff5 = NumberUtils.toDouble(bean.cutOff5),
            cutOff6 = NumberUtils.toDouble(bean.cutOff6),
            cutOff7 = NumberUtils.toDouble(bean.cutOff7),
            cutOff8 = NumberUtils.toDouble(bean.cutOff8),
            cutOffMax = NumberUtils.toDouble(bean.cutOffMax),
            noise1 = NumberUtils.toDouble(bean.noise1),
            noise2 = NumberUtils.toDouble(bean.noise2),
            noise3 = NumberUtils.toDouble(bean.noise3),
            noise4 = NumberUtils.toDouble(bean.noise4),
            noise5 = NumberUtils.toDouble(bean.noise5),
        )
    }

    fun fillEntity(bean: CardConfigBean, entity: CardConfig): CardConfig {
        return entity.copy(
            id = bean.id,
            name = bean.name,
            code = bean.code,
            type = bean.type,
            prodDate = bean.prodDate,
            endDate = bean.endDate,
            scanStart = NumberUtils.toDouble(bean.scanStart),
            scanEnd = NumberUtils.toDouble(bean.scanEnd),
            scanPPMM = NumberUtils.toInt(bean.scanPPMM),
            topList = Json.encodeToString(toTopEntityList(bean.topList)),
            varList = Json.encodeToString(toVarEntityList(bean.varList)),
            gmtCreated = LocalDateTime.now(),
            gmtModified = LocalDateTime.now(),
            ft0 = NumberUtils.toInt(bean.ft0),
            xt1 = NumberUtils.toInt(bean.xt1),
            ft1 = NumberUtils.toInt(bean.ft1),
            typeScore = NumberUtils.toDouble(bean.typeScore),
            scope = NumberUtils.toDouble(bean.scope),
            cAvg = NumberUtils.toDouble(bean.cAvg),
            cStd = NumberUtils.toDouble(bean.cStd),
            cMin = NumberUtils.toDouble(bean.cMin),
            cMax = NumberUtils.toDouble(bean.cMax),
            cutOff1 = NumberUtils.toDouble(bean.cutOff1),
            cutOff2 = NumberUtils.toDouble(bean.cutOff2),
            cutOff3 = NumberUtils.toDouble(bean.cutOff3),
            cutOff4 = NumberUtils.toDouble(bean.cutOff4),
            cutOff5 = NumberUtils.toDouble(bean.cutOff5),
            cutOff6 = NumberUtils.toDouble(bean.cutOff6),
            cutOff7 = NumberUtils.toDouble(bean.cutOff7),
            cutOff8 = NumberUtils.toDouble(bean.cutOff8),
            cutOffMax = NumberUtils.toDouble(bean.cutOffMax),
            noise1 = NumberUtils.toDouble(bean.noise1),
            noise2 = NumberUtils.toDouble(bean.noise2),
            noise3 = NumberUtils.toDouble(bean.noise3),
            noise4 = NumberUtils.toDouble(bean.noise4),
            noise5 = NumberUtils.toDouble(bean.noise5),
        )
    }

    @SuppressLint("DefaultLocale")
    fun fromEntity(entity: CardConfig): CardConfigBean {
        return CardConfigBean(
            id = entity.id,
            name = entity.name,
            code = entity.code,
            type = entity.type,
            prodDate = entity.prodDate,
            endDate = entity.endDate,
            scanStart = String.format("%.4f", entity.scanStart),
            scanEnd = String.format("%.4f", entity.scanEnd),
            scanPPMM = entity.scanPPMM.toString(),
            topList = fromTopEntityList(Json.decodeFromString(entity.topList)),
            varList = fromVarEntityList(Json.decodeFromString(entity.varList)),
            ft0 = entity.ft0.toString(),
            xt1 = entity.xt1.toString(),
            ft1 = entity.ft1.toString(),
            typeScore = String.format("%.4f", entity.typeScore),
            scope = String.format("%.4f", entity.scope),
            cAvg = String.format("%.4f", entity.cAvg),
            cStd = String.format("%.4f", entity.cStd),
            cMin = String.format("%.4f", entity.cMin),
            cMax = String.format("%.4f", entity.cMax),
            cutOff1 = String.format("%.4f", entity.cutOff1),
            cutOff2 = String.format("%.4f", entity.cutOff2),
            cutOff3 = String.format("%.4f", entity.cutOff3),
            cutOff4 = String.format("%.4f", entity.cutOff4),
            cutOff5 = String.format("%.4f", entity.cutOff5),
            cutOff6 = String.format("%.4f", entity.cutOff6),
            cutOff7 = String.format("%.4f", entity.cutOff7),
            cutOff8 = String.format("%.4f", entity.cutOff8),
            cutOffMax = String.format("%.4f", entity.cutOffMax),
            noise1 = String.format("%.4f", entity.noise1),
            noise2 = String.format("%.4f", entity.noise2),
            noise3 = String.format("%.4f", entity.noise3),
            noise4 = String.format("%.4f", entity.noise4),
            noise5 = String.format("%.4f", entity.noise5),
        )
    }

    private fun toTopEntityList(beanList: List<CardTopBean>): List<CardTop> {
        var cardTopList = ArrayList<CardTop>()
        for (cardTopBean in beanList) {
            cardTopList.add(toTopEntity(cardTopBean))
        }
        return cardTopList
    }

    private fun toTopEntity(bean: CardTopBean): CardTop {
        return CardTop(
            index = bean.index,
            id = bean.id,
            start = NumberUtils.toDouble(bean.start),
            end = NumberUtils.toDouble(bean.end),
            ctrl = bean.ctrl,
        )
    }

    private fun fromTopEntityList(entityList: List<CardTop>): List<CardTopBean> {
        var cardTopList = ArrayList<CardTopBean>()
        for (cardTop in entityList) {
            cardTopList.add(fromTopEntity(cardTop))
        }
        return cardTopList
    }

    @SuppressLint("DefaultLocale")
    private fun fromTopEntity(entity: CardTop): CardTopBean {
        return CardTopBean(
            index = entity.index,
            id = entity.id,
            start = String.format("%.4f", entity.start),
            end = String.format("%.4f", entity.end),
            ctrl = entity.ctrl,
        )
    }

    private fun toVarEntityList(beanList: List<CardVarBean>): List<CardVar> {
        var cardVarList = ArrayList<CardVar>()
        for (cardVarBean in beanList) {
            cardVarList.add(toVarEntity(cardVarBean))
        }
        return cardVarList
    }

    private fun toVarEntity(bean: CardVarBean): CardVar {
        return CardVar(
            index = bean.index,
            id = bean.id,
            type = bean.type,
            start = NumberUtils.toDouble(bean.start),
            end = NumberUtils.toDouble(bean.end),
            x0 = NumberUtils.toDouble(bean.x0),
            x1 = NumberUtils.toDouble(bean.x1),
            x2 = NumberUtils.toDouble(bean.x2),
            x3 = NumberUtils.toDouble(bean.x3),
            x4 = NumberUtils.toDouble(bean.x4),
        )
    }

    private fun fromVarEntityList(entityList: List<CardVar>): List<CardVarBean> {
        var cardVarList = ArrayList<CardVarBean>()
        for (cardVar in entityList) {
            cardVarList.add(fromVarEntity(cardVar))
        }
        return cardVarList
    }

    private fun fromVarEntity(entity: CardVar): CardVarBean {
        return CardVarBean(
            index = entity.index,
            id = entity.id,
            type = entity.type,
            start = String.format("%.4f", entity.start),
            end = String.format("%.4f", entity.end),
            x0 = String.format("%.6f", entity.x0),
            x1 = String.format("%.6f", entity.x1),
            x2 = String.format("%.6f", entity.x2),
            x3 = String.format("%.6f", entity.x3),
            x4 = String.format("%.6f", entity.x4),
        )
    }
}