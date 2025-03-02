package com.nightCloud.shanda.scoring;

import com.nightCloud.shanda.model.entity.App;
import com.nightCloud.shanda.model.entity.UserAnswer;
import com.nightCloud.shanda.service.QuestionService;
import com.nightCloud.shanda.service.ScoringResultService;

import javax.annotation.Resource;
import java.util.List;

/**
 * 自定义测评类应用评分策略
 */
@ScoringStrategyConfig(appType = 0, scoringStrategy = 0)
public class CustomTestScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        // 1.根据 id 查询到题目和题目结果信息

        // 2.统计用户每个选择对应的属性个数，如 I = 10 个，E = 5 个

        // 3.遍历每种评分结果，计算哪个结果的得分更高。

        // 4.构造返回值，填充答案对象的属性。

        return null;
    }
}
