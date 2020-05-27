/**
 * @author 蒋文龙(Vin)
 * @description
 * @date 2020/5/27
 */

import Stage from "./stage.js";
import Resource from "../tool/resource.js";
import Common from "../tool/common.js";


export default class Loading extends Stage {
    constructor() {
        super();

        //背景
        const bgImage = Resource.getImage("background_loading");
        this.createItem(function (ctx) {
            ctx.drawImage(bgImage,
                0, 0,
                bgImage.width, bgImage.height,
                0, 0,
                Resource.width(), Resource.height());
        });

        //文字
        this.missionNum = 0;
        this.updateMissionNum(0);
    }

    updateMissionNum(num) {
        this.missionNum += num;
        let displayNum = this.missionNum + 1;
        if (displayNum < 10) {
            displayNum = "0" + displayNum;
        } else {
            displayNum = "" + displayNum;
        }
        this.createOrUpdateItem(function (ctx) {
            Common.drawTitle(ctx, "MISSION " + displayNum);
        }, "title");
    }
}