package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Track;
import com.example.blogger.service.TrackService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tracks")
@CrossOrigin(origins = "*")
public class TrackController {
    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping
    public Result<List<Track>> list() {
        return Result.ok(trackService.list());
    }

    @GetMapping("/{id}")
    public Result<Track> get(@PathVariable String id) {
        return Result.ok(trackService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Track track) {
        trackService.save(track);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Track track) {
        track.setId(id);
        trackService.save(track);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        trackService.delete(id);
        return Result.ok(null);
    }
}
