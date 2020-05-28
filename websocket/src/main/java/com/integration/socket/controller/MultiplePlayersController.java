package com.integration.socket.controller;

import com.integration.util.CommonUtil;
import com.integration.util.model.ResultDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 蒋文龙(Vin)
 * @description
 * @date 2020/5/28
 */
@RestController
@RequestMapping("multiplePlayers")
public class MultiplePlayersController {

    @GetMapping("/getUserId")
    public ResultDto getUserId() {
        return new ResultDto(CommonUtil.getId());
    }

}
