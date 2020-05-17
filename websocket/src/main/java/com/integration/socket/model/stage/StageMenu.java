package com.integration.socket.model.stage;

import com.integration.dto.message.MessageDto;
import com.integration.dto.message.MessageType;
import com.integration.dto.map.ActionType;
import com.integration.socket.model.bo.BulletBo;
import com.integration.socket.model.bo.TankBo;
import com.integration.dto.map.ItemDto;
import com.integration.socket.service.MessageService;
import com.integration.util.object.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 蒋文龙(Vin)
 * @description 菜单
 * @date 2020/5/3
 */

@Slf4j
public class StageMenu extends BaseStage {

    private static final String MENU_DEFAULT_TYPE = "tankMenu";

    public StageMenu(MessageService messageService) {
        super(messageService);
    }

    @Override
    public String getRoomId() {
        return null;
    }

    @Override
    public void update() {
        for (Map.Entry<String, TankBo> kv : tankMap.entrySet()) {
            TankBo tankBo = kv.getValue();
            if (tankBo.getActionType() == ActionType.RUN) {
                tankBo.run(tankBo.getType().getSpeed());
            }
        }

        List<BulletBo> removeBullets = new ArrayList<>();
        for (Map.Entry<String, BulletBo> kv : bulletMap.entrySet()) {
            BulletBo bullet = kv.getValue();
            if (bullet.getLifeTime() == 0) {
                removeBullets.add(bullet);
                continue;
            }

            bullet.setLifeTime(bullet.getLifeTime() - 1);
            bullet.run();
        }
        for (BulletBo bullet : removeBullets) {
            bulletMap.remove(bullet.getId());
            if (tankMap.containsKey(bullet.getTankId())) {
                tankMap.get(bullet.getTankId()).addAmmoCount();
            }
            sendMessageToRoom(bullet.convertToDto(), MessageType.REMOVE_BULLET);
        }
    }

    @Override
    public void removeUser(String username) {
        removeTankFromTankId(username);
        sendMessageToRoom(getUserList(), MessageType.USERS);
    }

    @Override
    public List<String> getUserList() {
        List<String> users = new ArrayList<>();
        for (Map.Entry<String, TankBo> kv : tankMap.entrySet()) {
            users.add(kv.getKey());
        }
        return users;
    }

    public void addTank(MessageDto messageDto, String sendFrom) {
        ItemDto tankDto = ObjectUtil.readValue(messageDto.getMessage(), ItemDto.class);
        if (tankDto == null) {
            return;
        }
        tankDto.setId(sendFrom);
        tankDto.setTypeId(MENU_DEFAULT_TYPE);

        if (tankMap.containsKey(tankDto.getId())) {
            //单独发送同步消息
            sendMessageToUser(getUserList(), MessageType.USERS, sendFrom);
            sendMessageToUser(getTankList(), MessageType.TANKS, sendFrom);
            return;
        }

        TankBo tankBo = TankBo.convert(tankDto);
        tankMap.put(tankBo.getTankId(), tankBo);

        //收到单位，即将向所有人同步单位信息
        sendMessageToRoom(getUserList(), MessageType.USERS);
        sendMessageToRoom(getTankList(), MessageType.TANKS);

        sendReady(sendFrom);
    }

    private List<ItemDto> getTankList() {
        List<ItemDto> tankDtoList = new ArrayList<>();
        for (Map.Entry<String, TankBo> kv : tankMap.entrySet()) {
            tankDtoList.add(kv.getValue().convertToDto());
        }
        return tankDtoList;
    }
}
