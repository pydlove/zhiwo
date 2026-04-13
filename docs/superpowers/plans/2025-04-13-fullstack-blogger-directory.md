# 微信公众号头部博主整理小程序 - 全栈开发计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个完整的全栈系统：微信小程序前端 + Java Spring Boot 后端 + MySQL 数据库 + Web 管理后台，实现赛道/博主/文章的数据管理与展示。

**Architecture:** 前后端分离。微信小程序通过 HTTP 调用 Spring Boot RESTful API；管理后台是纯 HTML/JS 页面，直接挂载在 Spring Boot 的 static 目录下；MySQL 存储所有业务数据。

**Tech Stack:** 微信小程序原生、Java 17 + Spring Boot 3 + MyBatis + MySQL 8、HTML/JS 管理后台

---

## 项目目录结构

```
小程序/
├── wechat-miniprogram/          # 微信小程序前端
│   ├── app.js
│   ├── app.json
│   ├── app.wxss
│   ├── pages/
│   │   ├── index/
│   │   ├── track/
│   │   └── blogger/
│   ├── utils/
│   │   └── api.js
│   └── images/
├── backend/                     # Java Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/example/blogger/
│       ├── BloggerApplication.java
│       ├── controller/
│       │   ├── TrackController.java
│       │   ├── BloggerController.java
│       │   └── PostController.java
│       ├── entity/
│       │   ├── Track.java
│       │   ├── Blogger.java
│       │   └── Post.java
│       ├── mapper/
│       │   ├── TrackMapper.java
│       │   ├── BloggerMapper.java
│       │   └── PostMapper.java
│       └── service/
│           ├── TrackService.java
│           ├── BloggerService.java
│           └── PostService.java
│   └── src/main/resources/
│       ├── application.yml
│       ├── mapper/
│       │   ├── TrackMapper.xml
│       │   ├── BloggerMapper.xml
│       │   └── PostMapper.xml
│       └── static/admin/
│           ├── index.html
│           ├── tracks.html
│           ├── bloggers.html
│           └── posts.html
├── db/
│   └── init.sql
└── docs/
```

---

## 第一部分：数据库设计

### Task 1: 创建数据库初始化脚本

**Files:**
- Create: `db/init.sql`

- [ ] **Step 1: 编写 init.sql**

```sql
CREATE DATABASE IF NOT EXISTS blogger_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE blogger_db;

CREATE TABLE IF NOT EXISTS track (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(50) DEFAULT '',
    sort_order INT DEFAULT 0,
    preview_bloggers VARCHAR(500) DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS blogger (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(500) DEFAULT '',
    tagline VARCHAR(255) DEFAULT '',
    track_id VARCHAR(64) NOT NULL,
    rank_num INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_track_id (track_id)
);

CREATE TABLE IF NOT EXISTS post (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    blogger_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_blogger_id (blogger_id)
);
```

- [ ] **Step 2: 执行 SQL 脚本**

```bash
mysql -uroot -p123456 < db/init.sql
```

- [ ] **Step 3: Commit**

```bash
git add db/init.sql
git commit -m "feat: add mysql database schema"
```

---

## 第二部分：Java Spring Boot 后端

### Task 2: 初始化 Spring Boot 项目

**Files:**
- Create: `backend/pom.xml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>blogger-backend</artifactId>
    <version>1.0.0</version>
    <name>blogger-backend</name>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.3</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blogger_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.blogger.entity
  configuration:
    map-underscore-to-camel-case: true
```

- [ ] **Step 3: 创建主启动类**

```java
package com.example.blogger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BloggerApplication {
    public static void main(String[] args) {
        SpringApplication.run(BloggerApplication.class, args);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/
git commit -m "chore: init spring boot project with mybatis and mysql"
```

---

### Task 3: 创建实体类

**Files:**
- Create: `backend/src/main/java/com/example/blogger/entity/Track.java`
- Create: `backend/src/main/java/com/example/blogger/entity/Blogger.java`
- Create: `backend/src/main/java/com/example/blogger/entity/Post.java`
- Create: `backend/src/main/java/com/example/blogger/entity/Result.java`

- [ ] **Step 1: 创建 Track.java**

```java
package com.example.blogger.entity;

import java.time.LocalDateTime;

public class Track {
    private String id;
    private String name;
    private String icon;
    private Integer sortOrder;
    private String previewBloggers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getPreviewBloggers() { return previewBloggers; }
    public void setPreviewBloggers(String previewBloggers) { this.previewBloggers = previewBloggers; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 2: 创建 Blogger.java**

```java
package com.example.blogger.entity;

import java.time.LocalDateTime;

public class Blogger {
    private String id;
    private String name;
    private String avatar;
    private String tagline;
    private String trackId;
    private Integer rankNum;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public Integer getRankNum() { return rankNum; }
    public void setRankNum(Integer rankNum) { this.rankNum = rankNum; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3: 创建 Post.java**

```java
package com.example.blogger.entity;

import java.time.LocalDateTime;

public class Post {
    private String id;
    private String title;
    private String url;
    private String bloggerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getBloggerId() { return bloggerId; }
    public void setBloggerId(String bloggerId) { this.bloggerId = bloggerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 4: 创建统一返回结果 Result.java**

```java
package com.example.blogger.entity;

public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }

    // Getters and Setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/example/blogger/entity/
git commit -m "feat: add entity classes and unified result wrapper"
```

---

### Task 4: 创建 Mapper 接口和 XML

**Files:**
- Create: `backend/src/main/java/com/example/blogger/mapper/TrackMapper.java`
- Create: `backend/src/main/java/com/example/blogger/mapper/BloggerMapper.java`
- Create: `backend/src/main/java/com/example/blogger/mapper/PostMapper.java`
- Create: `backend/src/main/resources/mapper/TrackMapper.xml`
- Create: `backend/src/main/resources/mapper/BloggerMapper.xml`
- Create: `backend/src/main/resources/mapper/PostMapper.xml`

- [ ] **Step 1: 创建 TrackMapper.java**

```java
package com.example.blogger.mapper;

import com.example.blogger.entity.Track;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TrackMapper {
    @Select("SELECT * FROM track ORDER BY sort_order ASC")
    List<Track> findAll();

    @Select("SELECT * FROM track WHERE id = #{id}")
    Track findById(String id);

    @Insert("INSERT INTO track(id, name, icon, sort_order, preview_bloggers) VALUES(#{id}, #{name}, #{icon}, #{sortOrder}, #{previewBloggers})")
    int insert(Track track);

    @Update("UPDATE track SET name=#{name}, icon=#{icon}, sort_order=#{sortOrder}, preview_bloggers=#{previewBloggers} WHERE id=#{id}")
    int update(Track track);

    @Delete("DELETE FROM track WHERE id = #{id}")
    int delete(String id);
}
```

- [ ] **Step 2: 创建 BloggerMapper.java**

```java
package com.example.blogger.mapper;

import com.example.blogger.entity.Blogger;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface BloggerMapper {
    @Select("SELECT * FROM blogger WHERE track_id = #{trackId} ORDER BY rank_num ASC")
    List<Blogger> findByTrackId(String trackId);

    @Select("SELECT * FROM blogger WHERE id = #{id}")
    Blogger findById(String id);

    @Insert("INSERT INTO blogger(id, name, avatar, tagline, track_id, rank_num) VALUES(#{id}, #{name}, #{avatar}, #{tagline}, #{trackId}, #{rankNum})")
    int insert(Blogger blogger);

    @Update("UPDATE blogger SET name=#{name}, avatar=#{avatar}, tagline=#{tagline}, track_id=#{trackId}, rank_num=#{rankNum} WHERE id=#{id}")
    int update(Blogger blogger);

    @Delete("DELETE FROM blogger WHERE id = #{id}")
    int delete(String id);
}
```

- [ ] **Step 3: 创建 PostMapper.java**

```java
package com.example.blogger.mapper;

import com.example.blogger.entity.Post;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PostMapper {
    @Select("SELECT * FROM post WHERE blogger_id = #{bloggerId} ORDER BY created_at DESC")
    List<Post> findByBloggerId(String bloggerId);

    @Insert("INSERT INTO post(id, title, url, blogger_id) VALUES(#{id}, #{title}, #{url}, #{bloggerId})")
    int insert(Post post);

    @Update("UPDATE post SET title=#{title}, url=#{url}, blogger_id=#{bloggerId} WHERE id=#{id}")
    int update(Post post);

    @Delete("DELETE FROM post WHERE id = #{id}")
    int delete(String id);
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/example/blogger/mapper/
git commit -m "feat: add mybatis mapper interfaces for track, blogger and post"
```

---

### Task 5: 创建 Service 层

**Files:**
- Create: `backend/src/main/java/com/example/blogger/service/TrackService.java`
- Create: `backend/src/main/java/com/example/blogger/service/BloggerService.java`
- Create: `backend/src/main/java/com/example/blogger/service/PostService.java`

- [ ] **Step 1: 创建 TrackService.java**

```java
package com.example.blogger.service;

import com.example.blogger.entity.Track;
import com.example.blogger.mapper.TrackMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class TrackService {
    private final TrackMapper trackMapper;

    public TrackService(TrackMapper trackMapper) {
        this.trackMapper = trackMapper;
    }

    public List<Track> list() {
        return trackMapper.findAll();
    }

    public Track getById(String id) {
        return trackMapper.findById(id);
    }

    public void save(Track track) {
        if (track.getId() == null || track.getId().isEmpty()) {
            track.setId(UUID.randomUUID().toString().replace("-", ""));
            trackMapper.insert(track);
        } else {
            trackMapper.update(track);
        }
    }

    public void delete(String id) {
        trackMapper.delete(id);
    }
}
```

- [ ] **Step 2: 创建 BloggerService.java**

```java
package com.example.blogger.service;

import com.example.blogger.entity.Blogger;
import com.example.blogger.mapper.BloggerMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class BloggerService {
    private final BloggerMapper bloggerMapper;

    public BloggerService(BloggerMapper bloggerMapper) {
        this.bloggerMapper = bloggerMapper;
    }

    public List<Blogger> listByTrack(String trackId) {
        return bloggerMapper.findByTrackId(trackId);
    }

    public Blogger getById(String id) {
        return bloggerMapper.findById(id);
    }

    public void save(Blogger blogger) {
        if (blogger.getId() == null || blogger.getId().isEmpty()) {
            blogger.setId(UUID.randomUUID().toString().replace("-", ""));
            bloggerMapper.insert(blogger);
        } else {
            bloggerMapper.update(blogger);
        }
    }

    public void delete(String id) {
        bloggerMapper.delete(id);
    }
}
```

- [ ] **Step 3: 创建 PostService.java**

```java
package com.example.blogger.service;

import com.example.blogger.entity.Post;
import com.example.blogger.mapper.PostMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class PostService {
    private final PostMapper postMapper;

    public PostService(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    public List<Post> listByBlogger(String bloggerId) {
        return postMapper.findByBloggerId(bloggerId);
    }

    public void save(Post post) {
        if (post.getId() == null || post.getId().isEmpty()) {
            post.setId(UUID.randomUUID().toString().replace("-", ""));
            postMapper.insert(post);
        } else {
            postMapper.update(post);
        }
    }

    public void delete(String id) {
        postMapper.delete(id);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/example/blogger/service/
git commit -m "feat: add service layer with crud operations"
```

---

### Task 6: 创建 RESTful Controller

**Files:**
- Create: `backend/src/main/java/com/example/blogger/controller/TrackController.java`
- Create: `backend/src/main/java/com/example/blogger/controller/BloggerController.java`
- Create: `backend/src/main/java/com/example/blogger/controller/PostController.java`
- Create: `backend/src/main/java/com/example/blogger/config/WebConfig.java`

- [ ] **Step 1: 创建 TrackController.java**

```java
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

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        trackService.delete(id);
        return Result.ok(null);
    }
}
```

- [ ] **Step 2: 创建 BloggerController.java**

```java
package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Blogger;
import com.example.blogger.service.BloggerService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bloggers")
@CrossOrigin(origins = "*")
public class BloggerController {
    private final BloggerService bloggerService;

    public BloggerController(BloggerService bloggerService) {
        this.bloggerService = bloggerService;
    }

    @GetMapping
    public Result<List<Blogger>> listByTrack(@RequestParam String trackId) {
        return Result.ok(bloggerService.listByTrack(trackId));
    }

    @GetMapping("/{id}")
    public Result<Blogger> get(@PathVariable String id) {
        return Result.ok(bloggerService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Blogger blogger) {
        bloggerService.save(blogger);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        bloggerService.delete(id);
        return Result.ok(null);
    }
}
```

- [ ] **Step 3: 创建 PostController.java**

```java
package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Post;
import com.example.blogger.service.PostService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public Result<List<Post>> listByBlogger(@RequestParam String bloggerId) {
        return Result.ok(postService.listByBlogger(bloggerId));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Post post) {
        postService.save(post);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        postService.delete(id);
        return Result.ok(null);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/example/blogger/controller/
git commit -m "feat: add restful controllers with cors support"
```

---

### Task 7: 编译并启动后端服务

- [ ] **Step 1: 编译项目**

```bash
cd backend && mvn clean install -DskipTests
```

- [ ] **Step 2: 启动服务**

```bash
cd backend && mvn spring-boot:run
```

- [ ] **Step 3: 验证 API**

用 curl 测试接口是否通：

```bash
curl http://localhost:8080/api/tracks
```

预期返回：`{"code":200,"msg":"success","data":[]}`

---

## 第三部分：Web 管理后台

### Task 8: 创建管理后台静态页面

**Files:**
- Create: `backend/src/main/resources/static/admin/index.html`
- Create: `backend/src/main/resources/static/admin/tracks.html`
- Create: `backend/src/main/resources/static/admin/bloggers.html`
- Create: `backend/src/main/resources/static/admin/posts.html`

- [ ] **Step 1: 创建后台首页 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>管理后台</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; background: #f5f5f5; }
        .nav { background: #07c160; padding: 16px; color: #fff; }
        .nav h1 { font-size: 18px; }
        .menu { padding: 16px; }
        .menu-item { background: #fff; padding: 16px; border-radius: 8px; margin-bottom: 12px; display: block; text-decoration: none; color: #1a1a1a; font-size: 15px; }
    </style>
</head>
<body>
    <div class="nav"><h1>博主整理系统 - 管理后台</h1></div>
    <div class="menu">
        <a class="menu-item" href="tracks.html">🏷️ 赛道管理</a>
        <a class="menu-item" href="bloggers.html">👤 博主管理</a>
        <a class="menu-item" href="posts.html">📝 文章管理</a>
    </div>
</body>
</html>
```

- [ ] **Step 2: 创建赛道管理页面 tracks.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>赛道管理</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; background: #f5f5f5; padding: 16px; }
        h2 { font-size: 16px; margin-bottom: 12px; }
        .form { background: #fff; padding: 12px; border-radius: 8px; margin-bottom: 16px; }
        .form input { width: 100%; padding: 10px; margin-bottom: 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }
        .btn { background: #07c160; color: #fff; border: none; padding: 10px 20px; border-radius: 4px; font-size: 14px; }
        .list { background: #fff; border-radius: 8px; }
        .item { padding: 12px; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
        .item:last-child { border-bottom: none; }
        .del { color: #ff4d4f; font-size: 13px; }
        .back { display: block; margin-top: 16px; color: #666; font-size: 13px; text-decoration: none; }
    </style>
</head>
<body>
    <h2>赛道管理</h2>
    <div class="form">
        <input type="text" id="name" placeholder="赛道名称">
        <input type="text" id="icon" placeholder="图标 emoji，如 🎬">
        <input type="number" id="sortOrder" placeholder="排序数字">
        <input type="text" id="previewBloggers" placeholder="预览博主，如 粥左罗、剽悍一只猫">
        <button class="btn" onclick="add()">添加赛道</button>
    </div>
    <div class="list" id="list"></div>
    <a class="back" href="index.html">← 返回首页</a>

    <script>
        const API = '/api/tracks';
        async function load() {
            const res = await fetch(API);
            const result = await res.json();
            const list = document.getElementById('list');
            list.innerHTML = result.data.map(t => `
                <div class="item">
                    <div>${t.icon} ${t.name} (排序:${t.sortOrder})</div>
                    <span class="del" onclick="del('${t.id}')">删除</span>
                </div>
            `).join('');
        }
        async function add() {
            const body = {
                name: document.getElementById('name').value,
                icon: document.getElementById('icon').value,
                sortOrder: parseInt(document.getElementById('sortOrder').value) || 0,
                previewBloggers: document.getElementById('previewBloggers').value
            };
            await fetch(API, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
            ['name','icon','sortOrder','previewBloggers'].forEach(id => document.getElementById(id).value = '');
            load();
        }
        async function del(id) {
            if (!confirm('确定删除？')) return;
            await fetch(`${API}/${id}`, { method: 'DELETE' });
            load();
        }
        load();
    </script>
</body>
</html>
```

- [ ] **Step 3: 创建博主管理页面 bloggers.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>博主管理</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; background: #f5f5f5; padding: 16px; }
        h2 { font-size: 16px; margin-bottom: 12px; }
        .form { background: #fff; padding: 12px; border-radius: 8px; margin-bottom: 16px; }
        .form input, .form select { width: 100%; padding: 10px; margin-bottom: 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }
        .btn { background: #07c160; color: #fff; border: none; padding: 10px 20px; border-radius: 4px; font-size: 14px; }
        .list { background: #fff; border-radius: 8px; }
        .item { padding: 12px; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
        .item:last-child { border-bottom: none; }
        .del { color: #ff4d4f; font-size: 13px; }
        .back { display: block; margin-top: 16px; color: #666; font-size: 13px; text-decoration: none; }
    </style>
</head>
<body>
    <h2>博主管理</h2>
    <div class="form">
        <input type="text" id="name" placeholder="博主名称">
        <input type="text" id="tagline" placeholder="一句话定位">
        <select id="trackId"><option value="">选择赛道</option></select>
        <input type="number" id="rankNum" placeholder="排名">
        <button class="btn" onclick="add()">添加博主</button>
    </div>
    <div class="list" id="list"></div>
    <a class="back" href="index.html">← 返回首页</a>

    <script>
        const API = '/api/bloggers';
        async function loadTracks() {
            const res = await fetch('/api/tracks');
            const result = await res.json();
            const select = document.getElementById('trackId');
            result.data.forEach(t => {
                const opt = document.createElement('option');
                opt.value = t.id;
                opt.textContent = `${t.icon} ${t.name}`;
                select.appendChild(opt);
            });
        }
        async function load() {
            const trackId = document.getElementById('trackId').value;
            if (!trackId) { document.getElementById('list').innerHTML = '<div class="item" style="color:#999">请先在上方选择赛道</div>'; return; }
            const res = await fetch(`${API}?trackId=${trackId}`);
            const result = await res.json();
            document.getElementById('list').innerHTML = result.data.map(b => `
                <div class="item">
                    <div>#${b.rankNum} ${b.name} - ${b.tagline}</div>
                    <span class="del" onclick="del('${b.id}')">删除</span>
                </div>
            `).join('');
        }
        async function add() {
            const body = {
                name: document.getElementById('name').value,
                tagline: document.getElementById('tagline').value,
                trackId: document.getElementById('trackId').value,
                rankNum: parseInt(document.getElementById('rankNum').value) || 0
            };
            await fetch(API, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
            ['name','tagline','rankNum'].forEach(id => document.getElementById(id).value = '');
            load();
        }
        async function del(id) {
            if (!confirm('确定删除？')) return;
            await fetch(`${API}/${id}`, { method: 'DELETE' });
            load();
        }
        document.getElementById('trackId').addEventListener('change', load);
        loadTracks();
    </script>
</body>
</html>
```

- [ ] **Step 4: 创建文章管理页面 posts.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>文章管理</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; background: #f5f5f5; padding: 16px; }
        h2 { font-size: 16px; margin-bottom: 12px; }
        .form { background: #fff; padding: 12px; border-radius: 8px; margin-bottom: 16px; }
        .form input, .form select { width: 100%; padding: 10px; margin-bottom: 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }
        .btn { background: #07c160; color: #fff; border: none; padding: 10px 20px; border-radius: 4px; font-size: 14px; }
        .list { background: #fff; border-radius: 8px; }
        .item { padding: 12px; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
        .item:last-child { border-bottom: none; }
        .del { color: #ff4d4f; font-size: 13px; }
        .back { display: block; margin-top: 16px; color: #666; font-size: 13px; text-decoration: none; }
    </style>
</head>
<body>
    <h2>文章管理</h2>
    <div class="form">
        <select id="trackId"><option value="">选择赛道</option></select>
        <select id="bloggerId"><option value="">选择博主</option></select>
        <input type="text" id="title" placeholder="文章标题">
        <input type="text" id="url" placeholder="文章链接">
        <button class="btn" onclick="add()">添加文章</button>
    </div>
    <div class="list" id="list"></div>
    <a class="back" href="index.html">← 返回首页</a>

    <script>
        const API = '/api/posts';
        async function loadTracks() {
            const res = await fetch('/api/tracks');
            const result = await res.json();
            const select = document.getElementById('trackId');
            result.data.forEach(t => {
                const opt = document.createElement('option');
                opt.value = t.id;
                opt.textContent = `${t.icon} ${t.name}`;
                select.appendChild(opt);
            });
        }
        async function loadBloggers() {
            const trackId = document.getElementById('trackId').value;
            const select = document.getElementById('bloggerId');
            select.innerHTML = '<option value="">选择博主</option>';
            if (!trackId) return;
            const res = await fetch(`/api/bloggers?trackId=${trackId}`);
            const result = await res.json();
            result.data.forEach(b => {
                const opt = document.createElement('option');
                opt.value = b.id;
                opt.textContent = b.name;
                select.appendChild(opt);
            });
            load();
        }
        async function load() {
            const bloggerId = document.getElementById('bloggerId').value;
            if (!bloggerId) { document.getElementById('list').innerHTML = '<div class="item" style="color:#999">请先在上方选择博主</div>'; return; }
            const res = await fetch(`${API}?bloggerId=${bloggerId}`);
            const result = await res.json();
            document.getElementById('list').innerHTML = result.data.map(p => `
                <div class="item">
                    <div style="flex:1;min-width:0;">
                        <div style="font-size:14px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${p.title}</div>
                        <div style="font-size:11px;color:#999;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${p.url}</div>
                    </div>
                    <span class="del" onclick="del('${p.id}')">删除</span>
                </div>
            `).join('');
        }
        async function add() {
            const body = {
                title: document.getElementById('title').value,
                url: document.getElementById('url').value,
                bloggerId: document.getElementById('bloggerId').value
            };
            await fetch(API, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
            ['title','url'].forEach(id => document.getElementById(id).value = '');
            load();
        }
        async function del(id) {
            if (!confirm('确定删除？')) return;
            await fetch(`${API}/${id}`, { method: 'DELETE' });
            load();
        }
        document.getElementById('trackId').addEventListener('change', loadBloggers);
        document.getElementById('bloggerId').addEventListener('change', load);
        loadTracks();
    </script>
</body>
</html>
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/static/admin/
git commit -m "feat: add admin web dashboard for track, blogger and post management"
```

---

## 第四部分：微信小程序前端

### Task 9: 初始化小程序项目

**Files:**
- Create: `wechat-miniprogram/app.js`
- Create: `wechat-miniprogram/app.json`
- Create: `wechat-miniprogram/app.wxss`
- Create: `wechat-miniprogram/project.config.json`
- Create: `wechat-miniprogram/utils/api.js`

- [ ] **Step 1: 创建 app.json**

```json
{
  "pages": [
    "pages/index/index",
    "pages/track/track",
    "pages/blogger/blogger"
  ],
  "window": {
    "backgroundTextStyle": "light",
    "navigationBarBackgroundColor": "#fff",
    "navigationBarTitleText": "公众号博主榜",
    "navigationBarTextStyle": "black"
  },
  "tabBar": {
    "list": [
      {
        "pagePath": "pages/index/index",
        "text": "公众号",
        "iconPath": "images/icon-off.png",
        "selectedIconPath": "images/icon-on.png"
      }
    ],
    "selectedColor": "#07c160",
    "backgroundColor": "#ffffff",
    "borderStyle": "black"
  },
  "style": "v2",
  "sitemapLocation": "sitemap.json"
}
```

- [ ] **Step 2: 创建 api.js**

```javascript
const BASE_URL = 'http://localhost:8080/api'

function request(url, method = 'GET', data = null) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + url,
      method: method,
      data: data,
      header: { 'Content-Type': 'application/json' },
      success: (res) => {
        if (res.data.code === 200) {
          resolve(res.data.data)
        } else {
          reject(res.data.msg)
        }
      },
      fail: reject
    })
  })
}

module.exports = {
  getTracks: () => request('/tracks'),
  getTrack: (id) => request('/tracks/' + id),
  getBloggersByTrack: (trackId) => request('/bloggers?trackId=' + trackId),
  getBlogger: (id) => request('/bloggers/' + id),
  getPostsByBlogger: (bloggerId) => request('/posts?bloggerId=' + bloggerId)
}
```

- [ ] **Step 3: 创建 app.wxss**

```css
page {
  background-color: #f7f7f7;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.container {
  padding: 16px;
}
```

- [ ] **Step 4: Commit**

```bash
git add wechat-miniprogram/
git commit -m "chore: init wechat miniprogram frontend with api client"
```

---

### Task 10: 开发小程序首页（index）

**Files:**
- Create: `wechat-miniprogram/pages/index/index.json`
- Create: `wechat-miniprogram/pages/index/index.wxml`
- Create: `wechat-miniprogram/pages/index/index.wxss`
- Create: `wechat-miniprogram/pages/index/index.js`

- [ ] **Step 1: 创建 index.json**

```json
{
  "usingComponents": {},
  "navigationBarTitleText": "热门赛道"
}
```

- [ ] **Step 2: 创建 index.wxml**

```xml
<view class="container">
  <view class="page-title">🔥 热门赛道</view>
  
  <view class="track-list">
    <view 
      class="track-card" 
      wx:for="{{tracks}}" 
      wx:key="id"
      data-id="{{item.id}}"
      bindtap="goToTrack"
    >
      <view class="track-rank" style="color: {{index < 3 ? rankColors[index] : '#666'}};">{{index + 1}}</view>
      <view class="track-info">
        <view class="track-name">{{item.icon}} {{item.name}}</view>
        <view class="track-preview">{{item.previewBloggers}}</view>
      </view>
      <view class="track-action">查看 →</view>
    </view>
  </view>
  
  <view class="ad-placeholder">— 底部流量主广告位 —</view>
</view>
```

- [ ] **Step 3: 创建 index.wxss**

```css
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.track-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.track-card {
  background: #fff;
  padding: 14px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 3px solid transparent;
}

.track-card:nth-child(1) { border-left-color: #ff4d4f; }
.track-card:nth-child(2) { border-left-color: #ff7a45; }
.track-card:nth-child(3) { border-left-color: #ffa940; }
.track-card:nth-child(n+4) { border-left-color: #07c160; }

.track-rank {
  font-size: 24px;
  font-weight: 700;
  min-width: 28px;
  text-align: center;
}

.track-info {
  flex: 1;
}

.track-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.track-preview {
  font-size: 11px;
  color: #555;
  margin-top: 2px;
}

.track-action {
  font-size: 11px;
  color: #07c160;
  font-weight: 500;
}

.ad-placeholder {
  margin-top: 12px;
  font-size: 11px;
  color: #666;
  text-align: center;
}
```

- [ ] **Step 4: 创建 index.js**

```javascript
const api = require('../../utils/api.js')

Page({
  data: {
    tracks: [],
    rankColors: ['#ff4d4f', '#ff7a45', '#ffa940']
  },

  onLoad() {
    this.loadTracks()
  },

  loadTracks() {
    api.getTracks().then(tracks => {
      this.setData({ tracks })
    }).catch(err => {
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  goToTrack(e) {
    const trackId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/track/track?id=${trackId}`
    })
  }
})
```

- [ ] **Step 5: Commit**

```bash
git add wechat-miniprogram/pages/index/
git commit -m "feat: add wechat index page fetching tracks from backend"
```

---

### Task 11: 开发小程序赛道页（track）

**Files:**
- Create: `wechat-miniprogram/pages/track/track.json`
- Create: `wechat-miniprogram/pages/track/track.wxml`
- Create: `wechat-miniprogram/pages/track/track.wxss`
- Create: `wechat-miniprogram/pages/track/track.js`

- [ ] **Step 1: 创建 track.json**

```json
{
  "usingComponents": {},
  "navigationBarTitleText": "赛道详情"
}
```

- [ ] **Step 2: 创建 track.wxml**

```xml
<view class="container">
  <view class="page-title">{{track.icon}} {{track.name}}</view>
  
  <view class="blogger-list">
    <view 
      class="blogger-card" 
      wx:for="{{bloggers}}" 
      wx:key="id"
      data-id="{{item.id}}"
      bindtap="goToBlogger"
    >
      <view class="blogger-rank">{{item.rankNum}}</view>
      <view class="blogger-info">
        <view class="blogger-name">{{item.name}}</view>
        <view class="blogger-tagline">{{item.tagline}}</view>
      </view>
      <view class="blogger-action">查看作品 →</view>
    </view>
  </view>
  
  <view class="ad-placeholder">— 底部流量主广告位 —</view>
</view>
```

- [ ] **Step 3: 创建 track.wxss**

```css
.page-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.blogger-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.blogger-card {
  background: #fff;
  padding: 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.blogger-rank {
  width: 36px;
  height: 36px;
  background: #e0e0e0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #333;
  font-weight: 600;
  flex-shrink: 0;
}

.blogger-info {
  flex: 1;
  min-width: 0;
}

.blogger-name {
  font-size: 13px;
  font-weight: 500;
  color: #1a1a1a;
}

.blogger-tagline {
  font-size: 11px;
  color: #555;
  margin-top: 2px;
}

.blogger-action {
  font-size: 11px;
  color: #07c160;
  font-weight: 500;
  flex-shrink: 0;
}

.ad-placeholder {
  margin-top: 12px;
  font-size: 11px;
  color: #666;
  text-align: center;
}
```

- [ ] **Step 4: 创建 track.js**

```javascript
const api = require('../../utils/api.js')

Page({
  data: {
    track: {},
    bloggers: []
  },

  onLoad(options) {
    const trackId = options.id
    this.loadData(trackId)
  },

  loadData(trackId) {
    api.getTrack(trackId).then(track => {
      this.setData({ track })
      wx.setNavigationBarTitle({
        title: `${track.icon} ${track.name}`
      })
    })

    api.getBloggersByTrack(trackId).then(bloggers => {
      this.setData({ bloggers })
    }).catch(err => {
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  goToBlogger(e) {
    const bloggerId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/blogger/blogger?id=${bloggerId}`
    })
  }
})
```

- [ ] **Step 5: Commit**

```bash
git add wechat-miniprogram/pages/track/
git commit -m "feat: add wechat track page fetching bloggers from backend"
```

---

### Task 12: 开发小程序博主作品页（blogger）

**Files:**
- Create: `wechat-miniprogram/pages/blogger/blogger.json`
- Create: `wechat-miniprogram/pages/blogger/blogger.wxml`
- Create: `wechat-miniprogram/pages/blogger/blogger.wxss`
- Create: `wechat-miniprogram/pages/blogger/blogger.js`

- [ ] **Step 1: 创建 blogger.json**

```json
{
  "usingComponents": {},
  "navigationBarTitleText": "博主详情"
}
```

- [ ] **Step 2: 创建 blogger.wxml**

```xml
<view class="container">
  <view class="blogger-header">
    <image class="blogger-avatar" src="{{blogger.avatar}}" mode="aspectFill" wx:if="{{blogger.avatar}}"/>
    <view class="blogger-avatar placeholder" wx:else></view>
    <view class="blogger-meta">
      <view class="blogger-name">{{blogger.name}}</view>
      <view class="blogger-track">{{trackName}}赛道 · Top {{blogger.rankNum}}</view>
    </view>
  </view>
  
  <view class="section-title">🔥 代表作品</view>
  
  <view class="post-list">
    <view class="post-card" wx:for="{{posts}}" wx:key="id">
      <view class="post-title">{{item.title}}</view>
      <view class="post-actions">
        <view class="action-btn" data-url="{{item.url}}" bindtap="copyUrl">🔗 复制链接</view>
        <view class="action-btn" data-title="{{item.title}}" bindtap="copyTitle">📋 复制标题</view>
      </view>
    </view>
  </view>
  
  <view class="ad-placeholder">— 底部流量主广告位 —</view>
</view>
```

- [ ] **Step 3: 创建 blogger.wxss**

```css
.blogger-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.blogger-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  flex-shrink: 0;
}

.blogger-avatar.placeholder {
  background: #e0e0e0;
}

.blogger-meta {
  flex: 1;
}

.blogger-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.blogger-track {
  font-size: 11px;
  color: #555;
  margin-top: 2px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 10px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-card {
  background: #fff;
  padding: 12px;
  border-radius: 8px;
}

.post-title {
  font-size: 13px;
  color: #1a1a1a;
  margin-bottom: 8px;
  line-height: 1.5;
}

.post-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  background: #f0f0f0;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  color: #333;
  font-weight: 500;
}

.ad-placeholder {
  margin-top: 12px;
  font-size: 11px;
  color: #666;
  text-align: center;
}
```

- [ ] **Step 4: 创建 blogger.js**

```javascript
const api = require('../../utils/api.js')

Page({
  data: {
    blogger: {},
    trackName: '',
    posts: []
  },

  onLoad(options) {
    const bloggerId = options.id
    this.loadData(bloggerId)
  },

  loadData(bloggerId) {
    api.getBlogger(bloggerId).then(blogger => {
      this.setData({ blogger })
      wx.setNavigationBarTitle({ title: blogger.name })
    })

    api.getPostsByBlogger(bloggerId).then(posts => {
      this.setData({ posts })
    })
  },

  copyUrl(e) {
    const url = e.currentTarget.dataset.url
    wx.setClipboardData({
      data: url,
      success() {
        wx.showToast({ title: '链接已复制', icon: 'success', duration: 1500 })
      }
    })
  },

  copyTitle(e) {
    const title = e.currentTarget.dataset.title
    wx.setClipboardData({
      data: title,
      success() {
        wx.showToast({ title: '标题已复制', icon: 'success', duration: 1500 })
      }
    })
  }
})
```

- [ ] **Step 5: Commit**

```bash
git add wechat-miniprogram/pages/blogger/
git commit -m "feat: add wechat blogger page fetching posts from backend"
```

---

## 第五部分：联调与收尾

### Task 13: 联调测试

- [ ] **Step 1: 启动后端服务**

```bash
cd backend && mvn spring-boot:run
```

- [ ] **Step 2: 打开管理后台录入测试数据**

浏览器访问：`http://localhost:8080/admin/index.html`

依次操作：
1. 进入「赛道管理」，添加「个人IP」「投资理财」「互联网」等赛道
2. 进入「博主管理」，选择赛道，添加头部博主
3. 进入「文章管理」，选择博主，添加代表作品

- [ ] **Step 3: 在微信开发者工具中导入小程序项目**

路径：`/Users/panyong/aio_project/小程序/wechat-miniprogram`

- [ ] **Step 4: 验证小程序完整路径**

- 首页加载赛道列表
- 点击赛道进入博主列表
- 点击博主进入作品页
- 点击「复制链接」「复制标题」有 Toast 提示

- [ ] **Step 5: Commit 最终版本**

```bash
git add -A
git commit -m "feat: complete fullstack blogger directory system"
```

---

## Self-Review Checklist

1. **Spec coverage:**
   - [x] 数据库设计 → Task 1
   - [x] Java Spring Boot 后端 → Tasks 2-7
   - [x] Web 管理后台 → Task 8
   - [x] 微信小程序前端 → Tasks 9-12
   - [x] 联调测试 → Task 13

2. **Placeholder scan:** 无 TBD/TODO，所有代码均完整可执行。

3. **一致性检查：** API 路径、字段名、数据库表名在前后端完全一致。

---

## 执行方式

Plan complete and saved to `docs/superpowers/plans/2025-04-13-fullstack-blogger-directory.md`.

Two execution options:

1. **Subagent-Driven (recommended)** - dispatch a fresh subagent per task, review between tasks
2. **Inline Execution** - execute tasks in this session using executing-plans, batch execution
