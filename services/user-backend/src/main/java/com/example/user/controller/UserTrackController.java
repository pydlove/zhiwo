package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.User;
import com.example.user.entity.UserTrack;
import com.example.user.service.UserService;
import com.example.user.service.UserTrackService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserTrackController {
    private final UserTrackService userTrackService;
    private final UserService userService;

    public UserTrackController(UserTrackService userTrackService, UserService userService) {
        this.userTrackService = userTrackService;
        this.userService = userService;
    }

    @GetMapping("/{userId}/tracks")
    public Result<List<UserTrack>> list(@PathVariable String userId) {
        return Result.ok(userTrackService.listByUser(userId));
    }

    @PostMapping("/{userId}/tracks")
    public Result<Void> add(@PathVariable String userId, @RequestBody Map<String, String> req) {
        String trackId = req.get("trackId");
        if (trackId == null || trackId.isEmpty()) {
            return Result.error("赛道不能为空");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        Integer trackLimit = user.getTrackLimit();
        if (trackLimit != null && trackLimit > 0) {
            int current = userTrackService.countByUser(userId);
            if (current >= trackLimit) {
                return Result.error("您当前的权益最多可选择 " + trackLimit + " 个赛道，如需更多请联系管理员");
            }
        }
        UserTrack ut = new UserTrack();
        ut.setUserId(userId);
        ut.setTrackId(trackId);
        userTrackService.save(ut);
        return Result.ok(null);
    }

    @DeleteMapping("/{userId}/tracks/{trackId}")
    public Result<Void> delete(@PathVariable String userId, @PathVariable String trackId) {
        userTrackService.delete(userId, trackId);
        return Result.ok(null);
    }
}
