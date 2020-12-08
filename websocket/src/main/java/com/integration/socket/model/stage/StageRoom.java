package com.integration.socket.model.stage;

import com.integration.dto.map.ActionType;
import com.integration.dto.map.ItemDto;
import com.integration.dto.map.MapDto;
import com.integration.dto.map.MapUnitType;
import com.integration.dto.message.MessageType;
import com.integration.dto.room.GameStatusDto;
import com.integration.dto.room.GameStatusType;
import com.integration.dto.room.RoomDto;
import com.integration.dto.room.RoomType;
import com.integration.dto.room.TeamType;
import com.integration.socket.model.CollideType;
import com.integration.socket.model.ItemType;
import com.integration.socket.model.bo.BulletBo;
import com.integration.socket.model.bo.ItemBo;
import com.integration.socket.model.bo.MapBo;
import com.integration.socket.model.bo.MapMangerBo;
import com.integration.socket.model.bo.TankBo;
import com.integration.socket.model.bo.UserBo;
import com.integration.socket.model.bot.BaseBotBo;
import com.integration.socket.model.dto.StringCountDto;
import com.integration.socket.model.dto.TankTypeDto;
import com.integration.socket.model.event.BaseEvent;
import com.integration.socket.model.event.ClockEvent;
import com.integration.socket.model.event.CreateItemEvent;
import com.integration.socket.model.event.CreateTankEvent;
import com.integration.socket.model.event.IronKingEvent;
import com.integration.socket.model.event.LoadMapEvent;
import com.integration.socket.model.event.MessageEvent;
import com.integration.socket.repository.jooq.tables.records.UserRecord;
import com.integration.socket.service.MessageService;
import com.integration.socket.service.UserService;
import com.integration.util.CommonUtil;
import com.integration.util.model.CustomException;
import com.integration.util.time.TimeUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 蒋文龙(Vin)
 * @description
 * @date 2020/5/4
 */

@Slf4j
public class StageRoom extends BaseStage {

    private static final int MAX_ITEM_LIMIT = 3;
    private static final int TRY_TIMES_OF_CREATE_ITEM = 10;
    private static final int DEFAULT_SHIELD_TIME = 20 * 60;
    private static final int DEFAULT_SHIELD_TIME_FOR_NEW_TANK = 3 * 60;
    private static final int SCORE_COM_BOOM = 10;
    private static final int SCORE_PLAYER_BOOM = -30;
    private static final int SCORE_WIN = 500;
    private static final int SCORE_HARD_MODE = 100;

    private ConcurrentHashMap<String, List<String>> gridTankMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, List<String>> gridBulletMap = new ConcurrentHashMap<>();

    private Map<String, ItemBo> itemMap = new ConcurrentHashMap<>();

    private List<BaseEvent> eventList = new ArrayList<>();

    /**
     * 要删除的子弹列表，每帧刷新
     */
    private List<String> removeBulletIds = new ArrayList<>();

    /**
     * 要更新的坦克列表，保证坦克每秒和客户端同步一次
     */
    @Getter
    private Set<TankBo> syncTankList = new HashSet<>();

    /**
     * BOT列表
     */
    private Map<String, BaseBotBo> botMap = new ConcurrentHashMap<>();

    /**
     * 在通关后记录玩家属性
     */
    private Map<String, TankTypeDto> tankTypeSaveMap = new ConcurrentHashMap<>();

    @Getter
    private String roomId;

    private boolean hardMode;

    private UserBo creator;

    private MapMangerBo mapManger;

    private Random random = new Random();

    /**
     * 计分相关
     */
    private long missionStartTime;
    private int score = 0;
    private UserService userService;

    /**
     * 道具池
     */
    private List<ItemType> itemTypePool = new ArrayList<>();

    public StageRoom(RoomDto roomDto, UserBo creator, MapMangerBo mapManger, MessageService messageService, UserService userService) {
        super(messageService);
        this.roomId = roomDto.getRoomId();
        this.hardMode = roomDto.isHardMode();
        this.creator = creator;
        this.mapManger = mapManger;
        this.userService = userService;
        initItemPool(creator);

        init();
    }

    private void initItemPool(UserBo creator) {
        itemTypePool.addAll(Arrays.asList(ItemType.values()));
        if (!creator.hasRedStar()) {
            itemTypePool.remove(ItemType.RED_STAR);
        }
        if (!creator.hasGhost()) {
            itemTypePool.remove(ItemType.GHOST);
        }
        if (!creator.hasClock()) {
            itemTypePool.remove(ItemType.CLOCK);
        }
    }

    public RoomDto toDto() {
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomId(getRoomId());
        roomDto.setCreator(this.creator.getUsername());
        roomDto.setMapId(getMapId());
        roomDto.setSubId(getSubId());
        roomDto.setRoomType(getRoomType());
        roomDto.setUserCount(getUserCount());
        return roomDto;
    }

    private void init() {
        this.tankMap.clear();
        this.bulletMap.clear();
        this.itemMap.clear();
        this.gridBulletMap.clear();
        this.gridTankMap.clear();
        this.removeBulletIds.clear();
        this.eventList.clear();
        this.syncTankList.clear();
        this.missionStartTime = System.currentTimeMillis();
        this.gameStatus.init();

        this.eventList.add(new CreateItemEvent());
    }

    private int saveTankType() {
        this.tankTypeSaveMap.clear();
        for (Map.Entry<String, TankBo> kv : tankMap.entrySet()) {
            TankBo tankBo = kv.getValue();
            if (tankBo.isBot()) {
                continue;
            }
            this.tankTypeSaveMap.put(tankBo.getUserId(), tankBo.getType());
        }
        return this.tankTypeSaveMap.size();
    }

    private int getMapId() {
        return mapManger.getMapId();
    }

    private int getSubId() {
        return mapManger.getSubId();
    }

    private RoomType getRoomType() {
        return mapManger.getRoomType();
    }

    public MapBo getMapBo() {
        return mapManger.getMapBo();
    }

    private void addRemoveBulletIds(String id) {
        if (!this.removeBulletIds.contains(id)) {
            removeBulletIds.add(id);
        }
    }

    @Override
    public List<String> getUserList() {
        List<String> users = new ArrayList<>();
        for (Map.Entry<String, UserBo> kv : userMap.entrySet()) {
            users.add(kv.getKey());
        }
        return users;
    }

    @Override
    public void update() {
        processEvent();

        if (gameStatus.isPause()) {
            return;
        }

        for (Map.Entry<String, BaseBotBo> kv : botMap.entrySet()) {
            kv.getValue().update();
        }

        for (Map.Entry<String, BulletBo> kv : bulletMap.entrySet()) {
            updateBullet(kv.getValue());
        }
        removeBullets();

        for (Map.Entry<String, TankBo> kv : tankMap.entrySet()) {
            TankBo tankBo = kv.getValue();
            updateTank(tankBo);
        }
        syncTanks();
    }

    private void syncTanks() {
        if (syncTankList.isEmpty()) {
            return;
        }

        List<ItemDto> dtoList = new ArrayList<>();
        for (TankBo tank : syncTankList) {
            dtoList.add(tank.toDto());
        }
        sendMessageToRoom(dtoList, MessageType.TANKS);
        syncTankList.clear();
    }

    private void processEvent() {
        if (eventList.isEmpty()) {
            return;
        }

        for (int i = 0; i < eventList.size(); ++i) {
            BaseEvent event = eventList.get(i);
            if (event.getTimeout() == 0) {
                processEvent(event);
                eventList.remove(i);
                --i;
            } else {
                event.setTimeout(event.getTimeout() - 1);
            }
        }
    }

    private void processEvent(BaseEvent event) {
        //先检查是否执行
        if (!StringUtils.isEmpty(event.getUsernameCheck()) &&
                !this.userMap.containsKey(event.getUsernameCheck()) &&
                !this.botMap.containsKey(event.getUsernameCheck())) {
            return;
        }

        if (event instanceof CreateTankEvent) {
            CreateTankEvent createTankEvent = (CreateTankEvent) event;
            addNewTank(createTankEvent.getUser(), createTankEvent.getTankId());
            return;
        }

        if (event instanceof MessageEvent) {
            MessageEvent messageEvent = (MessageEvent) event;
            sendMessageToRoom(messageEvent.getContent(), messageEvent.getMessageType());
            return;
        }

        if (event instanceof ClockEvent) {
            if (gameStatus.getType() != GameStatusType.NORMAL) {
                gameStatus.setType(GameStatusType.NORMAL);
                sendMessageToRoom(gameStatus, MessageType.GAME_STATUS);
            }
            return;
        }

        if (event instanceof LoadMapEvent) {
            sendMessageToRoom(getMapBo().convertToDto(), MessageType.MAP);
            gameStatus.setType(GameStatusType.NORMAL);
            sendMessageToRoom(null, MessageType.SERVER_READY);
            //1 ~ 5 秒陆续出现坦克
            for (Map.Entry<String, UserBo> kv : userMap.entrySet()) {
                createTankForUser(kv.getValue(), random.nextInt(60 * 4) + 60);
            }
            for (Map.Entry<String, BaseBotBo> kv : botMap.entrySet()) {
                createTankForUser(kv.getValue().getBotUser(), random.nextInt(60 * 4) + 60);
            }
            return;
        }

        if (event instanceof IronKingEvent) {
            IronKingEvent ironKingEvent = (IronKingEvent) event;
            List<String> keys = ironKingEvent.getKeys();
            for (int i = 0; i < keys.size(); ++i) {
                String key = keys.get(i);
                if (!getMapBo().getUnitMap().containsKey(key)) {
                    keys.remove(i);
                    --i;
                } else {
                    getMapBo().getUnitMap().put(key, MapUnitType.BRICK);
                }
            }
            sendMessageToRoom(getMapBo().toDto(keys), MessageType.MAP);
            return;
        }

        if (event instanceof CreateItemEvent) {
            CreateItemEvent createItemEvent = (CreateItemEvent) event;
            if (itemMap.size() < MAX_ITEM_LIMIT) {
                ItemType itemType = itemTypePool.get(random.nextInt(itemTypePool.size()));
                for (int time = 0; time < TRY_TIMES_OF_CREATE_ITEM; ++time) {
                    Point grid = new Point();
                    grid.x = random.nextInt(getMapBo().getMaxGridX());
                    grid.y = random.nextInt(getMapBo().getMaxGridY());
                    String key = CommonUtil.generateKey(grid.x, grid.y);
                    if (getMapBo().getUnitMap().containsKey(key)) {
                        continue;
                    }
                    if (itemMap.containsKey(key)) {
                        continue;
                    }
                    Point pos = CommonUtil.getPointFromKey(key);
                    ItemBo itemBo = new ItemBo(key, pos, CommonUtil.getId(), itemType);
                    itemMap.put(key, itemBo);
                    sendMessageToRoom(Collections.singletonList(itemBo.toDto()), MessageType.ITEM);
                    break;
                }
            }
            createItemEvent.resetTimeout();
            this.eventList.add(createItemEvent);
        }
    }

    private void removeBullets() {
        if (removeBulletIds.isEmpty()) {
            return;
        }

        for (String bulletId : removeBulletIds) {
            BulletBo bullet = bulletMap.get(bulletId);
            if (bullet == null) {
                continue;
            }
            removeToGridBulletMap(bullet, bullet.getStartGridKey());
            removeToGridBulletMap(bullet, bullet.getEndGridKey());
            bulletMap.remove(bulletId);
            if (tankMap.containsKey(bullet.getTankId())) {
                tankMap.get(bullet.getTankId()).addAmmoCount();
            }
            sendMessageToRoom(bullet.convertToDto(), MessageType.REMOVE_BULLET);
        }
        removeBulletIds.clear();
    }

    private void updateBullet(BulletBo bullet) {
        if (this.removeBulletIds.contains(bullet.getId())) {
            return;
        }

        if (bullet.getLifeTime() == 0) {
            addRemoveBulletIds(bullet.getId());
            return;
        }

        if (collideWithAll(bullet)) {
            return;
        }

        bullet.setLifeTime(bullet.getLifeTime() - 1);
        bullet.run();

        String newStart = CommonUtil.generateGridKey(bullet.getX(), bullet.getY());
        if (newStart.equals(bullet.getStartGridKey())) {
            return;
        }

        removeToGridBulletMap(bullet, bullet.getStartGridKey());
        bullet.setStartGridKey(newStart);

        //newStartKey must equal endKey
        String newEnd = CommonUtil.generateEndGridKey(bullet.getX(), bullet.getY(), bullet.getOrientationType());
        bullet.setEndGridKey(newEnd);
        insertToGridBulletMap(bullet, newEnd);
    }

    /**
     * @param tankBo
     */
    private void updateTank(TankBo tankBo) {
        //查看是否为暂停状态
        if (gameStatus.getType() == GameStatusType.PAUSE_RED && tankBo.getTeamType() == TeamType.RED) {
            return;
        }
        if (gameStatus.getType() == GameStatusType.PAUSE_BLUE && tankBo.getTeamType() == TeamType.BLUE) {
            return;
        }

        boolean needUpdate = false;

        //填装弹药计算
        if (tankBo.getReloadTime() > 0) {
            tankBo.setReloadTime(tankBo.getReloadTime() - 1);
            if (tankBo.getReloadTime() == 0) {
                needUpdate = true;
            }
        }

        //护盾计算
        if (tankBo.getShieldTimeout() > 0) {
            tankBo.setShieldTimeout(tankBo.getShieldTimeout() - 1);
            if (!tankBo.hasShield()) {
                needUpdate = true;
            }
        }

        //移动计算
        if (tankBo.getActionType() == ActionType.RUN) {
            boolean canRun = true;
            //障碍物检测
            for (String key : tankBo.getGridKeyList()) {
                CollideType type = collideWithAll(tankBo, key);
                if (type != CollideType.COLLIDE_NONE) {
                    tankBo.setActionType(ActionType.STOP);
                    sendTankToRoom(tankBo, type.toString());
                    //已经发送了一次，重置标识
                    needUpdate = false;
                    canRun = false;
                    break;
                }
            }

            if (canRun) {
                if (catchItem(tankBo)) {
                    needUpdate = true;
                }
                tankBo.run(tankBo.getType().getSpeed());
                refreshTankGridMap(tankBo);
            }
        }

        if (needUpdate) {
            syncTankList.add(tankBo);
        }
    }

    private boolean catchItem(TankBo tankBo) {
        if (!canCatchItem(tankBo)) {
            return false;
        }

        for (String key : tankBo.getGridKeyList()) {
            if (!itemMap.containsKey(key)) {
                continue;
            }

            ItemBo itemBo = itemMap.get(key);
            double distance = Point.distance(tankBo.getX(), tankBo.getY(), itemBo.getPos().x, itemBo.getPos().y);
            if (distance <= CommonUtil.UNIT_SIZE) {
                return catchItem(tankBo, itemBo);
            }
        }
        return false;
    }

    private boolean canCatchItem(TankBo tankBo) {
        return getRoomType() != RoomType.PVE || tankBo.getTankId().equals(tankBo.getUserId()) || hardMode;
    }

    private boolean catchItem(TankBo tankBo, ItemBo itemBo) {
        switch (itemBo.getType()) {
            case STAR:
                if (!tankBo.levelUp()) {
                    return false;
                }
                itemMap.remove(itemBo.getPosKey());
                sendMessageToRoom(itemBo.getId(), MessageType.REMOVE_ITEM);
                return true;
            case RED_STAR:
                if (!tankBo.levelUpToTop()) {
                    return false;
                }
                itemMap.remove(itemBo.getPosKey());
                sendMessageToRoom(itemBo.getId(), MessageType.REMOVE_ITEM);
                return true;
            case SHIELD:
                tankBo.setShieldTimeout(tankBo.getShieldTimeout() + DEFAULT_SHIELD_TIME);
                itemMap.remove(itemBo.getPosKey());
                sendMessageToRoom(itemBo.getId(), MessageType.REMOVE_ITEM);
                return true;
            case LIFE:
                addLife(tankBo);
                itemMap.remove(itemBo.getPosKey());
                sendMessageToRoom(itemBo.getId(), MessageType.REMOVE_ITEM);
                return false;
            case KING:
                kingShield(tankBo.getTeamType());
                itemMap.remove(itemBo.getPosKey());
                sendMessageToRoom(itemBo.getId(), MessageType.REMOVE_ITEM);
                return false;
            case BULLET:
                tankBo.setBulletCount(tankBo.getBulletCount() + 1);
                itemMap.remove(itemBo.getPosKey());
                sendMessageToRoom(itemBo.getId(), MessageType.REMOVE_ITEM);
                return true;
            case GHOST:
                if (tankBo.isHasGhost()) {
                    return false;
                }
                tankBo.setHasGhost(true);
                itemMap.remove(itemBo.getPosKey());
                sendMessageToRoom(itemBo.getId(), MessageType.REMOVE_ITEM);
                return true;
            case CLOCK:
                clockEvent(tankBo.getTeamType());
                itemMap.remove(itemBo.getPosKey());
                sendMessageToRoom(itemBo.getId(), MessageType.REMOVE_ITEM);
                return false;
            default:
                return false;
        }
    }

    private void clockEvent(TeamType teamType) {
        if (teamType == TeamType.RED) {
            gameStatus.setType(GameStatusType.PAUSE_BLUE);
        } else {
            gameStatus.setType(GameStatusType.PAUSE_RED);
        }
        sendMessageToRoom(gameStatus, MessageType.GAME_STATUS);
        eventList.add(new ClockEvent());
    }

    /**
     * PVE模式下只作用于RED_KING
     *
     * @param teamType
     */
    private void kingShield(TeamType teamType) {
        MapUnitType kingType;
        if (teamType == TeamType.RED || getRoomType() == RoomType.PVE) {
            kingType = MapUnitType.RED_KING;
        } else {
            kingType = MapUnitType.BLUE_KING;
        }

        //寻找king
        List<String> kingKeys = new ArrayList<>();
        Map<String, MapUnitType> unitMap = getMapBo().getUnitMap();
        for (Map.Entry<String, MapUnitType> kv : unitMap.entrySet()) {
            if (kv.getValue() == kingType) {
                kingKeys.add(kv.getKey());
            }
        }

        //寻找要替换的key
        List<String> changeKeys = new ArrayList<>();
        for (String key : kingKeys) {
            Point p = CommonUtil.getGridPointFromKey(key);
            for (int x = p.x - 1; x <= p.x + 1; ++x) {
                for (int y = p.y - 1; y <= p.y + 1; ++y) {
                    if (x < 0 || x >= getMapBo().getMaxGridX()) {
                        continue;
                    }

                    if (y < 0 || y >= getMapBo().getMaxGridY()) {
                        continue;
                    }

                    String changeKey = CommonUtil.generateKey(x, y);
                    MapUnitType changeType = unitMap.getOrDefault(changeKey, MapUnitType.BRICK);
                    if (changeType == MapUnitType.BRICK || changeType == MapUnitType.BROKEN_BRICK) {
                        changeKeys.add(changeKey);
                    }
                }
            }
        }
        if (changeKeys.isEmpty()) {
            return;
        }

        //若是PVE模式则是拆掉基地附近的建筑
        if (getRoomType() == RoomType.PVE && teamType == TeamType.BLUE) {
            for (String changeKey : changeKeys) {
                removeMap(changeKey);
            }
        } else {
            for (String changeKey : changeKeys) {
                unitMap.put(changeKey, MapUnitType.IRON);
            }
            sendMessageToRoom(getMapBo().toDto(changeKeys), MessageType.MAP);
            this.eventList.add(new IronKingEvent(changeKeys));
        }

    }

    private void addLife(TankBo tankBo) {
        List<StringCountDto> life = getLifeMap(tankBo.getTeamType());

        if (!life.isEmpty()) {
            life.get(0).addValue(1);
        } else {
            life.add(new StringCountDto(tankBo.getType().getTypeId(), 1));

            //将观看模式的用户加入战场
            for (Map.Entry<String, UserBo> kv : userMap.entrySet()) {
                UserBo userBo = kv.getValue();
                if (isBot(userBo.getTeamType())) {
                    continue;
                }

                if (userBo.getTeamType() != tankBo.getTeamType()) {
                    continue;
                }

                if (tankMap.containsKey(userBo.getUsername())) {
                    continue;
                }

                this.eventList.add(new CreateTankEvent(userBo, 60 * 3));
                break;
            }
        }

        sendMessageToRoom(getMapBo().convertLifeCountToDto(), MessageType.MAP);
    }

    private CollideType collideWithAll(TankBo tankBo, String key) {
        Point grid = CommonUtil.getGridPointFromKey(key);
        if (grid.x < 0 || grid.y < 0 || grid.x >= getMapBo().getMaxGridX() || grid.y >= getMapBo().getMaxGridY()) {
            //超出范围，停止
            return CollideType.COLLIDE_BOUNDARY;
        }

        //幽灵状态下无视一切障碍
        if (tankBo.isHasGhost()) {
            return CollideType.COLLIDE_NONE;
        }

        if (collideForTank(getMapBo().getUnitMap().get(key))) {
            //有障碍物，停止
            return CollideType.COLLIDE_MAP;
        }

        if (collideWithTanks(tankBo, key)) {
            //有其他坦克，停止
            return CollideType.COLLIDE_TANK;
        }
        return CollideType.COLLIDE_NONE;
    }

    private boolean collideWithAll(BulletBo bullet) {
        if (bullet.getX() < 0 || bullet.getY() < 0 || bullet.getX() >= getMapBo().getWidth() || bullet.getY() >= getMapBo().getHeight()) {
            //超出范围
            addRemoveBulletIds(bullet.getId());
            return true;
        }

        //和地图场景碰撞检测
        if (collideForBullet(getMapBo().getUnitMap().get(bullet.getStartGridKey()))) {
            addRemoveBulletIds(bullet.getId());
            processMapWhenCatchAmmo(bullet.getStartGridKey(), bullet);
            return true;
        }

        //和坦克碰撞检测
        TankBo tankBo = collideWithTanks(bullet);
        if (tankBo != null) {
            addRemoveBulletIds(bullet.getId());
            if (!tankBo.hasShield()) {
                if (tankBo.levelDown()) {
                    sendTankToRoom(tankBo);
                } else {
                    removeTankFromTankId(tankBo.getTankId());
                }
            }
            return true;
        }

        //和子弹碰撞检测
        return collideWithBullets(bullet);
    }

    private void sendTankBombMessage(TankBo tank) {
        //不是玩家，过滤
        if (!tank.getTankId().equals(tank.getUserId())) {
            return;
        }

        sendMessageToRoom(String.format("%s 被摧毁了,等待3秒复活...", tank.getTankId()), MessageType.SYSTEM_MESSAGE);
    }

    private TankBo collideWithTanks(BulletBo bulletBo) {
        TankBo tankBo = collideWithTanks(bulletBo, bulletBo.getStartGridKey());
        if (tankBo != null) {
            return tankBo;
        }
        return collideWithTanks(bulletBo, bulletBo.getEndGridKey());
    }

    private TankBo collideWithTanks(BulletBo bulletBo, String key) {
        if (!gridTankMap.containsKey(key)) {
            return null;
        }

        for (String tankId : gridTankMap.get(key)) {
            TankBo tankBo = tankMap.get(tankId);
            //队伍相同，不检测
            if (tankBo.getTeamType() == bulletBo.getTeamType()) {
                continue;
            }
            double distance = Point.distance(tankBo.getX(), tankBo.getY(), bulletBo.getX(), bulletBo.getY());
            double minDistance = (CommonUtil.UNIT_SIZE + CommonUtil.AMMO_SIZE) / 2.0;
            if (distance <= minDistance) {
                return tankBo;
            }
        }
        return null;
    }

    private boolean collideWithBullets(BulletBo ammo) {
        if (collideWithBullets(ammo, ammo.getStartGridKey())) {
            return true;
        }

        return collideWithBullets(ammo, ammo.getEndGridKey());
    }

    private boolean collideWithBullets(BulletBo ammo, String key) {
        if (gridBulletMap.get(key) == null) {
            return false;
        }

        for (String id : gridBulletMap.get(key)) {
            if (id.equals(ammo.getId())) {
                continue;
            }

            BulletBo target = bulletMap.get(id);
            if (target == null || target.getTeamType() == ammo.getTeamType()) {
                continue;
            }

            double distance = Point.distance(ammo.getX(), ammo.getY(), target.getX(), target.getY());
            if (distance <= CommonUtil.AMMO_SIZE) {
                addRemoveBulletIds(ammo.getId());
                addRemoveBulletIds(target.getId());
                return true;
            }
        }
        return false;
    }

    private void processMapWhenCatchAmmo(String key, BulletBo bulletBo) {
        MapUnitType mapUnitType = getMapBo().getUnitMap().get(key);

        if (mapUnitType == MapUnitType.IRON && bulletBo.isBrokenIron()) {
            changeMap(key, MapUnitType.BROKEN_IRON);
            return;
        }

        if (mapUnitType == MapUnitType.BROKEN_IRON && bulletBo.isBrokenIron()) {
            removeMap(key);
            return;
        }

        if (mapUnitType == MapUnitType.BRICK) {
            changeMap(key, MapUnitType.BROKEN_BRICK);
            return;
        }

        if (mapUnitType == MapUnitType.BROKEN_BRICK) {
            removeMap(key);
            return;
        }

        if (mapUnitType == MapUnitType.RED_KING && bulletBo.getTeamType() != TeamType.RED) {
            removeMap(key);
            processGameOver(TeamType.BLUE);
            return;
        }

        if (mapUnitType == MapUnitType.BLUE_KING && bulletBo.getTeamType() != TeamType.BLUE) {
            removeMap(key);
            processGameOver(TeamType.RED);
        }
    }

    /**
     * 续关
     */
    public void restartPve(UserBo userBo) {
        if (gameStatus.getType() != GameStatusType.LOSE) {
            throw new CustomException("房间状态异常, 操作失败");
        }
        if (!mapManger.reload()) {
            throw new CustomException("地图读取异常, 操作失败");
        }

        sendMessageToRoom(String.format("%s 选择了续关,游戏将在5秒后重新开始...", userBo.getUsername()), MessageType.SYSTEM_MESSAGE);
        gameStatus.setType(GameStatusType.PAUSE);
        gameStatus.setMessage(String.format("多人模式 %d-%d", getMapId(), getSubId()));
        init();
        processNextMapEvent(5, "重新开始");
    }

    private void processGameOver(TeamType winTeam) {
        gameStatus.setType(GameStatusType.PAUSE);
        if (getRoomType() == RoomType.PVE && !processGameOverPve(winTeam)) {
            return;
        } else if (getRoomType() != RoomType.PVE && !processGameOverPvp(winTeam)) {
            return;
        }

        init();
        processNextMapEvent(10, "进入下一关");
    }

    private void processNextMapEvent(int loadSeconds, String tips) {
        int clearSeconds = loadSeconds - 2;
        for (int i = 1; i <= loadSeconds; ++i) {
            String content = String.format("%d秒后%s...", i, tips);
            MessageEvent messageEvent = new MessageEvent(content, MessageType.SYSTEM_MESSAGE);
            messageEvent.setTimeout((loadSeconds - i) * 60);
            this.eventList.add(messageEvent);
        }
        //清空地图
        MessageEvent cleanEvent = new MessageEvent(null, MessageType.CLEAR_MAP);
        cleanEvent.setTimeout(clearSeconds * 60);
        this.eventList.add(cleanEvent);

        //更改标题
        MessageEvent changeTitle = new MessageEvent(new GameStatusDto(GameStatusType.PAUSE, gameStatus.getMessage()), MessageType.GAME_STATUS);
        changeTitle.setTimeout(clearSeconds * 60);
        this.eventList.add(changeTitle);

        //加载地图
        LoadMapEvent loadEvent = new LoadMapEvent();
        loadEvent.setTimeout(loadSeconds * 60);
        this.eventList.add(loadEvent);
    }

    private boolean processGameOverPve(TeamType winTeam) {
        int saveLife = saveTankType();

        if (winTeam == TeamType.RED) {
            updateScoreAfterWin();
            gameStatus.setScore(this.score);
            gameStatus.setRank(userService.getRank(this.score));
            if (!mapManger.loadNextMapPve(saveLife)) {
                gameStatus.setMessage("恭喜全部通关");
                gameStatus.setType(GameStatusType.WIN);
                userService.saveRankForMultiplePlayers(this.creator, gameStatus);
                saveStage();
                getAndSaveCoin();
            } else {
                gameStatus.setMessage("恭喜通关");
            }
        } else {
            if (this.score < 0) {
                this.score = 0;
            }
            gameStatus.setScore(this.score);
            gameStatus.setRank(userService.getRank(this.score));
            gameStatus.setMessage("游戏失败");
            gameStatus.setType(GameStatusType.LOSE);
            userService.saveRankForMultiplePlayers(this.creator, gameStatus);
            getAndSaveCoin();
        }

        sendMessageToRoom(gameStatus, MessageType.GAME_STATUS);

        //修改标题，下次使用
        gameStatus.setMessage(String.format("多人模式 %d-%d", getMapId(), getSubId()));
        return gameStatus.getType() == GameStatusType.PAUSE;
    }

    /**
     * 保持通关记录
     */
    private void saveStage() {
        if (creator.getUserRecord() == null) {
            return;
        }

        UserRecord userRecord = creator.getUserRecord();
        if (userRecord.getStage() >= getMapId()) {
            return;
        }

        userRecord.setStage(getMapId());
        userRecord.update();
    }

    private void getAndSaveCoin() {
        for (Map.Entry<String, UserBo> kv : userMap.entrySet()) {
            userService.saveCoinFromScore(kv.getValue().getUserRecord(), score, false);
        }
    }

    private void updateScoreAfterWin() {
        int seconds = (int)((System.currentTimeMillis() - this.missionStartTime) / 1000);

        //困难模式加权
        int hardModeScore = hardMode ? SCORE_HARD_MODE : 0;

        int winScore = SCORE_WIN + hardModeScore - seconds;
        if (winScore > 0) {
            this.score += winScore;
        }
    }

    private boolean processGameOverPvp(TeamType winTeam) {
        gameStatus.setMessage(getTeam(winTeam) + "胜利!");

        if (!mapManger.loadRandomMapPvp()) {
            gameStatus.setType(GameStatusType.WIN);
        }

        sendMessageToRoom(gameStatus, MessageType.GAME_STATUS);

        //修改标题，下次使用
        gameStatus.setMessage("RED vs BLUE");
        return gameStatus.getType() == GameStatusType.PAUSE;
    }

    private void changeMap(String key, MapUnitType type) {
        getMapBo().getUnitMap().put(key, type);
        sendMessageToRoom(MapDto.convert(key, type), MessageType.MAP);
    }

    private void removeMap(String key) {
        getMapBo().getUnitMap().remove(key);
        sendMessageToRoom(key, MessageType.REMOVE_MAP);
    }

    private boolean collideWithTanks(TankBo tankBo, String key) {
        if (gridTankMap.containsKey(key)) {
            for (String tankId : gridTankMap.get(key)) {
                //跳过自己
                if (tankId.equals(tankBo.getTankId())) {
                    continue;
                }

                TankBo target = tankMap.get(tankId);
                if (collide(tankBo, target)) {
                    //和其他坦克相撞
                    return true;
                }
            }
        }
        return false;
    }

    private boolean collide(TankBo tank1, TankBo tank2) {
        double distance = Point.distance(tank1.getX(), tank1.getY(), tank2.getX(), tank2.getY());
        boolean isCollide = distance <= CommonUtil.UNIT_SIZE;
        switch (tank1.getOrientationType()) {
            case UP:
                if (isCollide && tank2.getY() < tank1.getY()) {
                    return true;
                }
                break;
            case DOWN:
                if (isCollide && tank2.getY() > tank1.getY()) {
                    return true;
                }
                break;
            case LEFT:
                if (isCollide && tank2.getX() < tank1.getX()) {
                    return true;
                }
                break;
            case RIGHT:
                if (isCollide && tank2.getX() > tank1.getX()) {
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }

    private boolean collideForTank(MapUnitType mapUnitType) {
        if (mapUnitType == null) {
            return false;
        }
        return mapUnitType != MapUnitType.GRASS;
    }

    private boolean collideForBullet(MapUnitType mapUnitType) {
        if (mapUnitType == null) {
            return false;
        }
        return mapUnitType != MapUnitType.GRASS && mapUnitType != MapUnitType.RIVER;
    }

    @Override
    void removeTankExtension(TankBo tankBo) {
        for (String key : tankBo.getGridKeyList()) {
            removeToGridTankMap(tankBo, key);
        }

        updateGameScore(tankBo);

        //check status
        if (checkGameStatusAfterTankBomb()) {
            return;
        }

        sendTankBombMessage(tankBo);

        //recreate
        UserBo userBo = this.userMap.get(tankBo.getUserId());
        if (userBo == null && this.botMap.containsKey(tankBo.getUserId())) {
            userBo = this.botMap.get(tankBo.getUserId()).getBotUser();
        }
        if (userBo != null) {
            this.eventList.add(new CreateTankEvent(userBo, tankBo.getTankId(), 60 * 3));
        }
    }

    private void updateGameScore(TankBo tankBo) {
        if (getRoomType() != RoomType.PVE) {
            return;
        }

        if (tankBo.getTeamType() == TeamType.RED) {
            this.score += SCORE_PLAYER_BOOM;
        } else {
            this.score += SCORE_COM_BOOM;
        }
    }

    private boolean checkGameStatusAfterTankBomb() {
        //游戏已经暂停
        if (gameStatus.isPause()) {
            return true;
        }

        //先判断候补
        boolean redAlive = !getMapBo().getPlayerLife().isEmpty();
        boolean blueAlive = !getMapBo().getComputerLife().isEmpty();

        //再判断在场
        for (Map.Entry<String, TankBo> kv : tankMap.entrySet()) {
            if (kv.getValue().getTeamType() == TeamType.RED) {
                redAlive = true;
            } else {
                blueAlive = true;
            }

            if (redAlive && blueAlive) {
                break;
            }
        }

        if (!redAlive) {
            processGameOver(TeamType.BLUE);
            return true;
        }
        if (!blueAlive) {
            processGameOver(TeamType.RED);
            return true;
        }
        return false;
    }

    @Override
    public void removeUser(String username) {
        if (!userMap.containsKey(username)) {
            return;
        }
        UserBo userBo = userMap.get(username);
        userMap.remove(username);
        removeTankFromUserId(username);
        if (getUserCount() == 0) {
            return;
        }
        sendStatusAndMessage(userBo, true);
    }

    private void sendStatusAndMessage(UserBo user, boolean leave) {
        sendMessageToRoom(getUserList(), MessageType.USERS);
        String message;
        if (leave) {
            message = String.format("%s 离开了房间 %s,当前房间人数: %d", generateUsernameWithTeam(user), roomId, getUserCount());
        } else {
            message = String.format("%s 加入了房间 %s,当前房间人数: %d", generateUsernameWithTeam(user), roomId, getUserCount());
        }
        sendMessageToRoom(message, MessageType.SYSTEM_MESSAGE);
    }

    private String generateUsernameWithTeam(UserBo userBo) {
        return String.format("%s[%s]", userBo.getUsername(), getTeam(userBo.getTeamType()));
    }

    private String getTeam(TeamType teamType) {
        String teamStr = "观看";
        switch (getRoomType()) {
            case EVE:
            case PVP:
                switch (teamType) {
                    case RED:
                        teamStr = "红队";
                        break;
                    case BLUE:
                        teamStr = "蓝队";
                        break;
                    default:
                        break;
                }
                break;
            case PVE:
                switch (teamType) {
                    case RED:
                        teamStr = "玩家";
                        break;
                    case BLUE:
                        teamStr = "AI";
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return teamStr;
    }

    public void addBot(BaseBotBo bot) {
        bot.setStage(this);
        botMap.put(bot.getId(), bot);

        //每有一个玩家加入，玩家生命+1
        if (getRoomType() == RoomType.PVE && bot.getTeamType() == TeamType.RED) {
            getMapBo().addPlayerLife(1);
        }

        createTankForUser(bot.getBotUser(), 60 * 3);
    }

    public void addUser(UserBo userBo) {
        //TODO - 暂停状态不允许加入,需要后期优化
        if (gameStatus.isPause()) {
            return;
        }

        userMap.put(userBo.getUsername(), userBo);
        userBo.setRoomId(this.roomId);
        sendStatusAndMessage(userBo, false);

        //每有一个玩家加入，玩家生命+1
        if (getRoomType() == RoomType.PVE && userBo.getTeamType() == TeamType.RED) {
            getMapBo().addPlayerLife(1);
        }

        //发送场景信息
        sendMessageToUser(getMapBo().convertToDto(), MessageType.MAP, userBo.getUsername());
        sendMessageToUser(getTankList(), MessageType.TANKS, userBo.getUsername());
        sendMessageToUser(getBulletList(), MessageType.BULLET, userBo.getUsername());
        sendMessageToUser(getItemPool(), MessageType.ITEM, userBo.getUsername());

        createTankForUser(userBo, 60 * 3);
        sendReady(userBo.getUsername());
    }

    private void createTankForUser(UserBo userBo, int timeoutForPlayer) {
        if (isBot(userBo.getTeamType())) {
            int count = getMapBo().getComputerStartCount();
            for (int i = 0; i < count; ++i) {
                this.eventList.add(new CreateTankEvent(userBo, CommonUtil.getId(), 60 * random.nextInt(count)));
            }
            return;
        }

        this.eventList.add(new CreateTankEvent(userBo, timeoutForPlayer));
    }

    private void addNewTank(UserBo userBo, String tankId) {
        if (userBo.getTeamType() == TeamType.VIEW) {
            return;
        }

        List<StringCountDto> lifeMap = getLifeMap(userBo.getTeamType());
        if (lifeMap.isEmpty()) {
            if (!isBot(userBo.getTeamType())) {
                sendMessageToRoom(String.format("没有剩余生命值, 玩家 %s 将变成观看模式", userBo.getUsername()), MessageType.SYSTEM_MESSAGE);
            }
            return;
        }

        TankBo tankBo = new TankBo();
        tankBo.setTankId(tankId);
        tankBo.setUserId(userBo.getUsername());
        tankBo.setTeamType(userBo.getTeamType());

        //设定类型
        TankTypeDto initType = getTankType(lifeMap, userBo);
        TankTypeDto saveType = this.tankTypeSaveMap.get(userBo.getUsername());
        if (saveType != null) {
            tankBo.setType(saveType);
            this.tankTypeSaveMap.remove(userBo.getUsername());
        } else {
            tankBo.setType(initType);
        }

        if (!tankBo.isBot()) {
            tankBo.setShieldTimeout(DEFAULT_SHIELD_TIME_FOR_NEW_TANK);
        }
        setStartPoint(tankBo);
        tankBo.setBulletCount(tankBo.getType().getAmmoMaxCount());
        tankMap.put(tankBo.getTankId(), tankBo);

        sendTankToRoom(tankBo);
    }

    /**
     * 根据队伍获得类型，并且更新life
     *
     * @param lifeMap
     * @return
     */
    private TankTypeDto getTankType(List<StringCountDto> lifeMap, UserBo userBo) {
        StringCountDto pair = lifeMap.get(0);
        int lastCount = pair.getValue() - 1;
        if (lastCount == 0) {
            lifeMap.remove(0);
        } else {
            pair.setValue(lastCount);
        }
        sendMessageToRoom(getMapBo().convertLifeCountToDto(), MessageType.MAP);
        String tankType = pair.getKey();
        if (isBot(userBo.getTeamType())) {
            return TankTypeDto.getTankType(tankType);
        }
        if (userBo.getUserRecord() == null) {
            return TankTypeDto.getTankType(tankType);
        }
        UserRecord userRecord = userBo.getUserRecord();
        if (!StringUtils.isEmpty(userRecord.getTankType()) &&
                userRecord.getTankTypeExpired() != null &&
                userRecord.getTankTypeExpired().after(TimeUtil.now())) {
            tankType = userRecord.getTankType();
        }
        return TankTypeDto.getTankType(tankType);
    }

    private List<StringCountDto> getLifeMap(TeamType teamType) {
        if (teamType == TeamType.RED) {
            return getMapBo().getPlayerLife();
        } else {
            return getMapBo().getComputerLife();
        }
    }

    private List<ItemDto> getTankList() {
        List<ItemDto> tankDtoList = new ArrayList<>();
        for (Map.Entry<String, TankBo> kv : tankMap.entrySet()) {
            tankDtoList.add(kv.getValue().toDto());
        }
        return tankDtoList;
    }

    private List<ItemDto> getBulletList() {
        List<ItemDto> bulletDtoList = new ArrayList<>();
        for (Map.Entry<String, BulletBo> kv : bulletMap.entrySet()) {
            bulletDtoList.add(kv.getValue().convertToDto());
        }
        return bulletDtoList;
    }

    private List<ItemDto> getItemPool() {
        List<ItemDto> itemDtoList = new ArrayList<>();
        for (Map.Entry<String, ItemBo> kv : itemMap.entrySet()) {
            itemDtoList.add(kv.getValue().toDto());
        }
        return itemDtoList;
    }

    private void setStartPoint(TankBo tankBo) {
        String posStr;
        if (tankBo.getTeamType() == TeamType.RED) {
            posStr = getMapBo().getPlayerStartPoints().get(random.nextInt(getMapBo().getPlayerStartPoints().size()));
        } else {
            posStr = getMapBo().getComputerStartPoints().get(random.nextInt(getMapBo().getComputerStartPoints().size()));
        }
        tankBo.getGridKeyList().add(posStr);
        insertToGridTankMap(tankBo, posStr);
        Point point = CommonUtil.getPointFromKey(posStr);
        tankBo.setX(point.getX());
        tankBo.setY(point.getY());
    }

    private void insertToGridTankMap(TankBo tankBo, String key) {
        if (!gridTankMap.containsKey(key)) {
            gridTankMap.put(key, new ArrayList<>());
        }
        if (!gridTankMap.get(key).contains(tankBo.getTankId())) {
            gridTankMap.get(key).add(tankBo.getTankId());
        }
    }

    private void removeToGridTankMap(TankBo tankBo, String key) {
        if (!gridTankMap.containsKey(key)) {
            return;
        }
        gridTankMap.get(key).remove(tankBo.getTankId());
        if (gridTankMap.get(key).isEmpty()) {
            gridTankMap.remove(key);
        }
    }

    private void insertToGridBulletMap(BulletBo bullet, String key) {
        if (!gridBulletMap.containsKey(key)) {
            gridBulletMap.put(key, new ArrayList<>());
        }
        if (!gridBulletMap.get(key).contains(bullet.getId())) {
            gridBulletMap.get(key).add(bullet.getId());
        }
    }

    private void removeToGridBulletMap(BulletBo bullet, String key) {
        if (!gridBulletMap.containsKey(key)) {
            return;
        }
        gridBulletMap.get(key).remove(bullet.getId());
        if (gridBulletMap.get(key).isEmpty()) {
            gridBulletMap.remove(key);
        }
    }

    @Override
    void updateTankControlExtension(TankBo tankBo, ItemDto tankDto) {
        //TODO - 距离检测，防止作弊

        if (tankDto.getX() != null && tankDto.getY() != null) {
            tankBo.setX(tankDto.getX());
            tankBo.setY(tankDto.getY());
        }
        refreshTankGridMap(tankBo);
    }

    private void refreshTankGridMap(TankBo tankBo) {
        List<String> newKeys = tankBo.generateGridKeyList();
        for (String oldKey : tankBo.getGridKeyList()) {
            if (!newKeys.contains(oldKey)) {
                removeToGridTankMap(tankBo, oldKey);
            }
        }
        for (String newKey : newKeys) {
            if (!tankBo.getGridKeyList().contains(newKey)) {
                insertToGridTankMap(tankBo, newKey);
            }
        }
        tankBo.setGridKeyList(newKeys);
    }

    @Override
    void processTankFireExtension(BulletBo bullet) {
        String startKey = CommonUtil.generateGridKey(bullet.getX(), bullet.getY());
        bullet.setStartGridKey(startKey);
        insertToGridBulletMap(bullet, startKey);

        String endKey = CommonUtil.generateEndGridKey(bullet.getX(), bullet.getY(), bullet.getOrientationType());
        bullet.setEndGridKey(endKey);
        insertToGridBulletMap(bullet, endKey);
    }

    private boolean isBot(TeamType teamType) {
        if (getRoomType() == RoomType.EVE) {
            return true;
        }
        return getRoomType() == RoomType.PVE && teamType == TeamType.BLUE;
    }
}
