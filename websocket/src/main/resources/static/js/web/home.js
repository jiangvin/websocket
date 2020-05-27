
/**
 * @author 蒋文龙(Vin)
 * @description
 * @date 2020/5/26
 */

import Resource from "../share/tool/resource.js";
import Stage from "../share/stage/stage.js"
import Common from "../share/tool/common.js";
import Control from "../share/tool/control.js";

export default class Home extends Stage {
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

        //标题
        this.createItem(function (context) {
            context.font = 'bold 55px Helvetica';
            context.textAlign = 'center';
            context.textBaseline = 'middle';
            context.fillStyle = '#FFF';
            context.fillText('坦克世界', Resource.width() / 2, 40);
        });

        //绑定事件
        const buttonEvent = function (e) {
            const name = $('#input').val();

            //检测是否输入名字
            if (name === "") {
                Common.addMessage("名字不能为空!", "#ff0000");
                return;
            }

            const isTouch = e.currentTarget.id === "button2";
            Control.setControlMode(isTouch);

            Resource.setUsername(name);
            $('#input-name-div').css("visibility", "hidden");
            Common.nextStage();
        };
        $('#button1').bind('click', buttonEvent);
        $('#button2').bind('click', buttonEvent);
    }
}