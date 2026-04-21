package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.UserTrack;
import com.example.blogger.service.UserTrackService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserTrackController {
    private final UserTrackService userTrackService;

    public UserTrackController(UserTrackService userTrackService) {
        this.userTrackService = userTrackService;
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
