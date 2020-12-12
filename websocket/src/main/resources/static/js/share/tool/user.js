/**
 * @author 蒋文龙(Vin)
 * @description
 * @date 2020/5/26
 */
import Resource from "./resource.js";

export default class User {
    constructor() {
        this.userId = "";
        this.coin = 0;
        this.score = 0;
        this.originalUserId = null;
        this.deviceId = null;
        this.deviceName = null;
        this.redStarExpired = null;
        this.ghostExpired = null;
        this.clockExpired = null;

        this.tankType = "tank01";
        this.tankTypeExpired = null;

        this.stage = 1;
        this.hardStage = 0;
        this.rank = 0;
    }

    setData(data) {
        if (!data) {
            return;
        }

        this.setUserId(data.username);
        this.coin = data.coin;
        this.redStarExpired = data.redStarExpired;
        this.ghostExpired = data.ghostExpired;
        this.clockExpired = data.clockExpired;
        this.tankType = data.tankType;
        this.tankTypeExpired = data.tankTypeExpired;
        this.stage = data.stage;
        this.hardStage = data.hardStage;

        //更新排名和积分
        if (data.rank) {
            this.rank = data.rank;
            this.score = data.score;
        }
    }

    setUserId(userId) {
        this.userId = userId;
        if (!this.originalUserId) {
            this.originalUserId = userId;
        }
    }

    hasRedStar() {
        if (Resource.isDebug()) {
            return true;
        }
        return this.redStarExpired && this.redStarExpired > new Date().getTime();
    }

    hasGhost() {
        if (Resource.isDebug()) {
            return true;
        }
        return this.ghostExpired && this.ghostExpired > new Date().getTime();
    }

    hasClock() {
        if (Resource.isDebug()) {
            return true;
        }
        return this.clockExpired && this.clockExpired > new Date().getTime();
    }

    getTankType() {
        if (!this.tankTypeExpired || this.tankTypeExpired < new Date().getTime()) {
            return "tank01";
        }
        return this.tankType;
    }
}