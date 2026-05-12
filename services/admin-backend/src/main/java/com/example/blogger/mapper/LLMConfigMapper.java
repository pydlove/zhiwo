package com.example.blogger.mapper;

import com.example.blogger.entity.LLMConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LLMConfigMapper {

    @Select("SELECT * FROM tu_llm_config ORDER BY provider")
    List<LLMConfig> findAll();

    @Select("SELECT * FROM tu_llm_config WHERE provider = #{provider}")
    LLMConfig findByProvider(@Param("provider") String provider);

    @Select("SELECT * FROM tu_llm_config WHERE is_active = 1 LIMIT 1")
    LLMConfig findActive();

    @Insert("INSERT INTO tu_llm_config(provider, api_key, model, is_active) VALUES(#{provider}, #{apiKey}, #{model}, #{isActive}) " +
            "ON DUPLICATE KEY UPDATE api_key=#{apiKey}, model=#{model}, is_active=#{isActive}")
    int save(LLMConfig config);

    @Update("UPDATE tu_llm_config SET is_active = 0 WHERE provider != #{provider}")
    int deactivateOthers(@Param("provider") String provider);
}
