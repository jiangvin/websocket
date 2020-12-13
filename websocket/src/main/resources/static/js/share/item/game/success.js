/**
 * @author 蒋文龙(Vin)
 * @description 胜利信息板
 * @date 2020/12/12
 */
import Resource from "../../tool/resource.js";
import Sound from "../../tool/sound.js";

export default class Success {
    constructor(stage, score, rank) {
        this.stage = stage;
        this.score = score < 0 ? 0 : score;
        this.rank = rank < 0 ? "--" : rank;

        this.init();
        Sound.win();
    }

    init() {
        this.boardSize = {
            w: 627,
            h: 398,
            speed: 0.05,
            scale: 0
        };
        this.lightSize = {
            w: 1127,
            h: 1127,
            speed: 1 / 180,
            angle: 0
        };

        const imgLight = Resource.getImage("success_light");
        const imgBoard = Resource.getImage("success");

        this.item = this.stage.createItem({
            draw: ctx => {
                //黑色蒙蔽
                ctx.globalAlpha = 0.6;
                ctx.fillStyle = '#000';
                ctx.fillRect(0, 0, Resource.width(), Resource.height());
                ctx.globalAlpha = 1;

                this.center = {
                    x: Resource.width() / 2,
                    y: Resource.height() * .45
                };

                //light
                if (this.boardSize.scale === 1) {
                    this.lightSize.angle += this.lightSize.speed;
                    this.rotate(ctx, this.center, Math.PI * this.lightSize.angle);
                    ctx.drawImage(
                        imgLight,
                        0, 0,
                        imgLight.width, imgLight.height,
                        this.center.x - this.lightSize.w / 2,
                        this.center.y - this.lightSize.h / 2,
                        this.lightSize.w, this.lightSize.h
                    );
                    this.rotate(ctx, this.center, -Math.PI * this.lightSize.angle);
                }

                //board
                if (this.boardSize.scale < 1) {
                    this.boardSize.scale += this.boardSize.speed;
                } else {
                    this.boardSize.scale = 1;
                }
                ctx.drawImage(
                    imgBoard,
                    0, 0,
                    imgBoard.width, imgBoard.height,
                    this.center.x - this.boardSize.w / 2 * this.boardSize.scale,
                    this.center.y - this.boardSize.h / 2 * this.boardSize.scale,
                    this.boardSize.w * this.boardSize.scale,
                    this.boardSize.h * this.boardSize.scale
                );

                //info
                ctx.font = '60px gameFont';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                ctx.fillStyle = '#FFF';
                ctx.fillText("当前得分: " + this.score,
                    this.center.x,
                    this.center.y + 300);

                ctx.fillText("当前排名: " + this.rank,
                    this.center.x,
                    this.center.y + 430);
            }
        });
    }

    rotate(ctx, center, angle) {
        ctx.translate(center.x, center.y);
        ctx.rotate(angle);
        ctx.translate(-center.x, -center.y);
    }
}