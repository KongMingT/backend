package com.nightCloud.shanda.scoring;

import com.nightCloud.shanda.model.entity.App;
import com.nightCloud.shanda.model.entity.UserAnswer;

import java.util.List;

@ScoringStrategyConfig(appType = 1, scoringStrategy = 0)
public class CustomScoreScoringStrategy implements ScoringStrategy {
    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        // 1. 根据 id 查询到题目和题目结果信息（按分数降序排序）

        // 2. 统计用户的总得分

        // 3. 遍历得分结果，找到第一个用户分数大于得分范围的结果，作为最终结果

        // 4. 构造返回值，填充答案对象的属性

        return null;
    }
}
