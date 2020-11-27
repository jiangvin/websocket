/**
 * @author 蒋文龙(Vin)
 * @description
 * @date 2020/5/26
 */

import Stage from "./stage.js";
import Resource from "../tool/resource.js";
import Common from "../tool/common.js";
import ControlUnit from "../item/controlunit.js";
import RoomInfo from "../item/roominfo.js";

export default class Menu extends Stage {
    constructor() {
        super();

        this.createFullScreenItem("menu_background");
        this.createBullet();
        this.createDoor();
        this.createFullScreenItem("menu_wall");

        //信息
        this.createItem({
            draw: function (ctx) {
                ctx.font = 'bold 13px Arial';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                ctx.fillStyle = '#FFF';
                const pos = {
                    x: 172 + Resource.getOffset().x,
                    y: 36 + Resource.getOffset().y
                };
                //姓名
                ctx.fillText(Resource.getUser().userId, pos.x, pos.y);
                //金币
                ctx.fillText(Resource.getUser().coin, pos.x + 200, pos.y);
                //排名
                ctx.fillText(Resource.getUser().rank, pos.x + 422, pos.y);
                //积分
                ctx.fillText(Resource.getUser().score, pos.x + 633, pos.y);
            }
        });

        //排行榜打开按钮
        const buttonOpenRankBoard = new ControlUnit(
            Resource.generateClientId(),
            {x: 86, y: 372},
            {x: 188, y: 518},
            function () {
                Common.nextStage();
            }
        );
        this.controlUnits.set(buttonOpenRankBoard.id, buttonOpenRankBoard);
    }

    createBullet() {
        const bulletMap = new Map();
        const bulletWidth = 80;

        this.createItem({
            draw: function (ctx) {
                //create bullet
                if (Math.floor(Math.random() * 50) === 0) {
                    const bullet = {
                        id: Resource.generateClientId(),
                        x: Math.floor(Math.random() * Resource.displayW()),
                        y: Resource.displayH() / 2,
                        scale: Math.random() + 0.1
                    };
                    bulletMap.set(bullet.id, bullet);
                }

                //draw
                bulletMap.forEach(bullet => {
                    bullet.x -= 10 * bullet.scale;
                    bullet.y -= 10 * bullet.scale;
                    const width = bulletWidth * bullet.scale;
                    ctx.displayCenter("menu_bullet", bullet.x, bullet.y, width);
                    if (bullet.x <= -width) {
                        bulletMap.delete(bullet.id);
                    }
                })
            }
        })
    }

    createDoor() {
        const speed = 0.5;

        this.createItem({
            draw: function (ctx) {
                const doorStatus = this.stage.doorStatus;
                if (doorStatus.enterDoor1) {
                    if (doorStatus.indexDoor1 < 22) {
                        doorStatus.indexDoor1 += speed;
                    } else {
                        Common.gotoStage("mission", new RoomInfo());
                    }
                }
                if (doorStatus.enterDoor2 && doorStatus.indexDoor2 < 22) {
                    doorStatus.indexDoor2 += speed;
                }

                const doorSize = {
                    w: 180,
                    h: 200
                };

                ctx.displayTopLeft(
                    "menu_door_" + Math.floor(doorStatus.indexDoor1),
                    244,
                    292,
                    doorSize.w,
                    doorSize.h);
                ctx.displayTopLeft(
                    "menu_door_" + Math.floor(doorStatus.indexDoor2),
                    576,
                    292,
                    doorSize.w,
                    doorSize.h);
            }
        });

        //事件处理
        const thisMenu = this;
        //单人模式
        const singleMode = new ControlUnit(
            Resource.generateClientId(),
            {x: 278, y: 307},
            {x: 374, y: 470},
            function () {
                thisMenu.doorStatus.enterDoor1 = true;
            }
        );
        this.controlUnits.set(singleMode.id, singleMode);

        //多人模式
        const multipleMode = new ControlUnit(
            Resource.generateClientId(),
            {x: 610, y: 307},
            {x: 706, y: 470},
            function () {
                thisMenu.doorStatus.enterDoor2 = true;
            }
        );
        this.controlUnits.set(multipleMode.id, multipleMode);
    }

    init() {
        this.doorStatus = {
            indexDoor1: 0,
            enterDoor1: false,
            indexDoor2: 0,
            enterDoor2: false
        };
    }

    getId() {
        return "menu";
    }
}