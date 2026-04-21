package com.example.user.mapper;

import com.example.user.entity.Config;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ConfigMapper {
    @Select("SELECT * FROM tu_config ORDER BY config_key")
    List<Config> findAll();

    @Select("SELECT * FROM tu_config WHERE config_key = #{key}")
    Config findByKey(String key);

    @Insert("INSERT INTO tu_config(config_key, config_value) VALUES(#{configKey}, #{configValue}) ON DUPLICATE KEY UPDATE config_value=#{configValue}")
    int save(Config config);
}
