<template>
  <div id="addQuestionPage">
    <h2 style="margin-bottom: 32px">设置题目</h2>
    <a-form
      :style="{
        width: '480px',
      }"
      label-align="left"
      auto-label-width
      :model="questionContent"
      @submit="handleSubmit"
    >
      <!--      {{ questionContent }}-->
      <a-form-item label="应用 id">
        {{ appId }}
      </a-form-item>
      <a-form-item label="题目列表" :content-flex="false" :merge-props="false">
        <a-space size="medium">
          <a-button @click="addQuestion(questionContent.length)">
            底部添加题目
          </a-button>
          <!-- AI 生成抽屉 -->
          <AiGenerateQuestionDrawer
            :appId="appId"
            :onSuccess="onAiGenerateSuccess"
            :onSSESuccess="onAiGenerateSuccessSSE"
            :onSSEClose="onSSEClose"
            :onSSEStart="onSSEStart"
          />
        </a-space>

        <!-- 遍历每道题目 -->
        <div v-for="(question, index) of questionContent" :key="index">
          <!-- 题目信息展示 -->
          <a-space size="large">
            <h3>题目 {{ index + 1 }}</h3>
            <a-button size="mini" @click="addQuestion(index + 1)">
              添加题目
            </a-button>
            <a-button
              size="mini"
              status="danger"
              @click="deleteQuestion(index)"
            >
              删除题目
            </a-button>
          </a-space>
          <a-form-item :label="`题目 ${index + 1} 标题`">
            <a-input v-model="question.title" placeholder="请输入标题" />
          </a-form-item>
          <!--题目选项-->
          <!-- 展示题目选项 -->
          <a-space size="large">
            <h4>题目 {{ index + 1 }} 选项列表</h4>
            <a-button
              size="mini"
              @click="addQuestionOption(question, question.options.length)"
            >
              底部添加选项
            </a-button>
          </a-space>
          <!-- 遍历每个选项 -->
          <a-form-item
            v-for="(option, optionIndex) of question.options"
            :label="`选项 ${optionIndex + 1}`"
            :key="optionIndex"
            :content-flex="false"
            :merge-props="false"
          >
            <a-form-item label="选项 key">
              <a-input v-model="option.key" placeholder="请输入选项 key" />
            </a-form-item>
            <a-form-item label="选项值">
              <a-input v-model="option.value" placeholder="请输入选项值" />
            </a-form-item>
            <a-form-item label="选项结果">
              <a-input v-model="option.result" placeholder="请输入选项结果" />
            </a-form-item>
            <a-form-item label="选项得分">
              <a-input-number
                v-model="option.score"
                placeholder="请输入选项得分"
              />
            </a-form-item>
            <a-space>
              <a-button
                size="mini"
                @click="addQuestionOption(question, optionIndex + 1)"
              >
                添加选项
              </a-button>
              <a-button
                size="mini"
                status="danger"
                @click="deleteQuestionOption(question, optionIndex as number)"
              >
                删除选项
              </a-button>
            </a-space>
          </a-form-item>
          <!--题目选项结尾-->
        </div>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 120px">
          提交
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { defineProps, ref, watchEffect, withDefaults } from "vue";
import API from "@/api";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import {
  addQuestionUsingPost,
  editQuestionUsingPost,
  listQuestionVoByPageUsingPost,
} from "@/api/questionController";
import AiGenerateQuestionDrawer from "@/views/add/components/AiGenerateQuestionDrawer.vue";

const router = useRouter();

interface Props {
  appId: string;
}

const props = withDefaults(defineProps<Props>(), {
  appId: () => {
    return "";
  },
});

/**
 * 题目内容结构(理解为题目列表）
 */
const questionContent = ref<API.QuestionContentDTO[]>([]);

const oldQuestion = ref<API.QuestionVO>();

/**
 * 添加题目
 * @param index
 */
const addQuestion = (index: number) => {
  questionContent.value.splice(index, 0, {
    title: "",
    options: [],
  });
};

/**
 * 删除题目
 * @param index
 */
const deleteQuestion = (index: number) => {
  questionContent.value.splice(index, 1);
};

/**
 * 添加题目选项
 * @param index
 */
const addQuestionOption = (question: API.QuestionContentDTO, index: number) => {
  if (!question.options) {
    question.options = [];
  }
  question.options.splice(index, 0, {
    key: "",
    value: "",
  });
};

/**
 * 删除题目选项
 * @param index
 */
const deleteQuestionOption = (
  question: API.QuestionContentDTO,
  index: number
) => {
  if (!question.options) {
    question.options = [];
  }
  questionContent.value.splice(index, 1);
};

/**
 * 加载数据
 */
const loadData = async () => {
  if (!props.appId) {
    return;
  }
  const res = await listQuestionVoByPageUsingPost({
    appId: props.appId as any,
    current: 1,
    pageSize: 1,
    sortField: "createTime",
    sortOrder: "descend",
  });
  if (res.data.code === 0 && res.data.data?.records) {
    oldQuestion.value = res.data.data?.records[0];
    if (oldQuestion.value) {
      questionContent.value = oldQuestion.value.questionContent ?? [];
    }
  } else {
    message.error("获取数据失败" + res.data.message);
  }
};

// 获取旧数据
watchEffect(() => {
  loadData();
});

/**
 * 提交
 */
const handleSubmit = async () => {
  if (!props.appId || !questionContent.value) {
    return;
  }
  let res;
  // 如果是修改
  if (oldQuestion.value?.id) {
    res = await editQuestionUsingPost({
      id: oldQuestion.value?.id,
      questionContent: questionContent.value,
    });
  } else {
    // 创建
    res = await addQuestionUsingPost({
      appId: props.appId as any,
      questionContent: questionContent.value,
    });
  }
  if (res.data.code === 0) {
    message.success("创建成功，即将跳转到应用详情页");
    setTimeout(() => {
      router.push(`/app/detail/${props.appId}`);
    }, 3000);
  } else {
    message.error("创建失败，" + res.data.message);
  }
};

/**
 * AI 生成题目后执行
 */
const onAiGenerateSuccess = (result: API.QuestionContentDTO[]) => {
  message.success(`题目生成成功,共生成${result.length} 道题目`);
  questionContent.value = [...questionContent.value, ...result];
};

/**
 * AI 生成题目后执行?(SSE)
 */
const onAiGenerateSuccessSSE = (result: API.QuestionContentDTO) => {
  questionContent.value = [...questionContent.value, result];
};

/**
 * SSE 开始生成
 * @param event
 */
const onSSEStart = (event: any) => {
  message.success("开始生成");
};

/**
 * SSE 生成完毕
 * @param event
 */
const onSSEClose = (event: any) => {
  message.success("生成完毕");
};
</script>
