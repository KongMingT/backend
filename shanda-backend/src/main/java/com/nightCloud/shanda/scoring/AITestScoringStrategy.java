package com.nightCloud.shanda.scoring;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nightCloud.shanda.manager.AiManager;
import com.nightCloud.shanda.model.dto.question.QuestionAnswerDTO;
import com.nightCloud.shanda.model.dto.question.QuestionContentDTO;
import com.nightCloud.shanda.model.entity.*;
import com.nightCloud.shanda.model.vo.QuestionVO;
import com.nightCloud.shanda.service.QuestionService;
import com.nightCloud.shanda.service.ScoringResultService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI 测评类应用评分策略
 */
@ScoringStrategyConfig(appType = 1, scoringStrategy = 1)
public class AITestScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private AiManager aiManager;

//    @Resource
//    private RedissonClient redissonClient;

    // 分布式锁的 key
    private static final String AI_ANSWER_LOCK = "AI_ANSWER_LOCK";

    /**
     * AI 评分结果本地缓存
     */
    private final Cache<String, String> answerCacheMap =
            Caffeine.newBuilder().initialCapacity(1024)
                    // 缓存5分钟移除
                    .expireAfterAccess(5L, TimeUnit.MINUTES)
                    .build();


    private static final String AI_TEST_SCORING_SYSTEM_MESSAGE = "你是一位严谨的判题专家，我会给你如下信息：\n" +
            "```\n" +
            "应用名称，\n" +
            "【【【应用描述】】】，\n" +
            "题目和用户回答的列表：格式为 [{\"title\": \"题目\",\"answer\": \"用户回答\"}]\n" +
            "```\n" +
            "\n" +
            "请你根据上述信息，按照以下步骤来对用户进行评价：\n" +
            "1. 要求：需要给出一个明确的评价结果，包括评价名称（尽量简短）和评价描述（尽量详细，大于 200 字）\n" +
            "2. 严格按照下面的 json 格式输出评价名称和评价描述\n" +
            "```\n" +
            "{\"resultName\": \"评价名称\", \"resultDesc\": \"评价描述\"}\n" +
            "```\n" +
            "3. 返回格式必须为 JSON 对象";


    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        Long appId = app.getId();
        String jsonStr = JSONUtil.toJsonStr(choices);
        String cacheKey = buildCacheKey(appId, jsonStr);
        String answerJson = answerCacheMap.getIfPresent(cacheKey);
        // 命中缓存则直接返回结果
        if (StrUtil.isNotBlank(answerJson)) {
            // 构造返回值，填充答案对象的属性
            UserAnswer userAnswer = JSONUtil.toBean(answerJson, UserAnswer.class);
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(jsonStr);
            return userAnswer;
        }

//        // 定义锁
//        RLock lock = redissonClient.getLock(AI_ANSWER_LOCK + cacheKey);
//
//        try {
//            // 竞争锁
//            boolean isLock = lock.tryLock(3, 15, TimeUnit.SECONDS);
//            // 没抢到锁，强行返回
//            if (!isLock) {
//                throw new RuntimeException("当前用户正在测评中，请稍后再试");
//            }
//            // 抢到锁后，执行后续业务逻辑
            // 1.根据 id 查询到题目和题目结果信息
            Question question = questionService.getOne(
                    Wrappers.lambdaQuery(Question.class)
                            .eq(Question::getAppId, app.getId()));
            QuestionVO questionVO = QuestionVO.objToVo(question);
            List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();
            // 2.调用AI 获取结果
            // 封装 Prompt
            String userMessage = getAiTestScoringUserMessage(app, questionContent, choices);
            // AI 生成
            String result = aiManager.doSyncStableRequest(AI_TEST_SCORING_SYSTEM_MESSAGE, userMessage);
            // 截取需要的 JSON 信息
            int start = result.indexOf("{");
            int end = result.lastIndexOf("}");
            String json = result.substring(start, end + 1);

            // 将结果缓存
            answerCacheMap.put(cacheKey, json);

            // 3.构造返回值，填充答案对象的属性。
            UserAnswer userAnswer = JSONUtil.toBean(json, UserAnswer.class);
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(jsonStr);

            return userAnswer;
//        } finally {
//            if (lock != null && lock.isLocked()){
//                // 只有本人才能释放锁
//                if (lock.isHeldByCurrentThread()) {
//                    lock.unlock();
//                }
//            }
//        }

    }

    /**
     * AI 评分用户消息封装
     *
     * @param app
     * @param questionContentDTOList
     * @param choices
     * @return
     */
    private String getAiTestScoringUserMessage(App app, List<QuestionContentDTO> questionContentDTOList, List<String> choices) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n");
        List<QuestionAnswerDTO> questionAnswerDTOList = new ArrayList<>();
        for (int i = 0; i < questionContentDTOList.size(); i++) {
            QuestionAnswerDTO questionAnswerDTO = new QuestionAnswerDTO();
            questionAnswerDTO.setTitle(questionContentDTOList.get(i).getTitle());
            questionAnswerDTO.setUserAnswer(choices.get(i));
            questionAnswerDTOList.add(questionAnswerDTO);
        }
        userMessage.append(JSONUtil.toJsonStr(questionAnswerDTOList));
        return userMessage.toString();
    }

    /**
     * 缓存 key 构造
     *
     * @param appId
     * @param choicesStr
     * @return
     */
    private String buildCacheKey(Long appId, String choicesStr) {
        return DigestUtil.md5Hex(appId + ":" + choicesStr);
    }

}
