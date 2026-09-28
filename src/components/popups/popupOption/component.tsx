import React from "react";
import "./popupOption.css";

import { PopupOptionProps } from "./interface";
import {
  getEnabledPopupOptionKeys,
  popupOptionMap,
  PopupOptionKey,
} from "../../../constants/popupList";
import {
  ConfigService,
  HighlightUtil,
} from "../../../assets/lib/kookit-extra-browser.min";
import toast from "react-hot-toast";
import {
  getSelection,
  getSelectionSentence,
  searchInTheBook,
} from "../../../utils/reader/mouseEvent";
import copy from "copy-text-to-clipboard";
import { getIframeDoc } from "../../../utils/reader/docUtil";
import { openExternalUrl } from "../../../utils/common";
import { createHighlight } from "../../../utils/reader/noteUtil";
import { Tooltip } from "react-tooltip";

declare var window: any; //`declare`：TS 关键字，**声明存在某个变量 / 类型，不生成 JS 代码**。
//1. `var window`：声明全局变量名字叫 window。
// 2. `: any`：把这个 window 的类型标记为`any`，意味着**关闭 window 的类型校验**，window 上随便加属性、随便访问，TS 不会警告



class PopupOption extends React.Component<PopupOptionProps> {
  highlightUtil: any;
  //类里面的**实例成员变量**（属性名），后面实例可以用 `this.highlightUtil` 访问
  //`: any`：类型注解，代表**这个变量任意类型，关闭 TS 类型检查**
  constructor(props: PopupOptionProps) {
    super(props);
    this.highlightUtil = new HighlightUtil(ConfigService);

  }
  handleNote = () => { //笔记弹窗
    this.props.handleMenuMode("note");
    this.props.handleOpenMenu(true);
  };
  handleCopy = () => {   //复制
    const format = this.props.currentBook.format;
    let text = getSelection(format);
    if (!text) return;
    if (
      format === "PDF" &&
      !ConfigService.getAllListConfig("convertPDFBooks").includes(
        this.props.currentBook.key
      )
    ) {
      text = text.split("\n").join(" ").trim();
    }
    let copied = false;
    const docs = getIframeDoc(format);
    for (let i = 0; i < docs.length && !copied; i++) {
      const doc = docs[i];
      if (!doc) continue;
      const sel = doc.getSelection();
      if (!sel || sel.rangeCount === 0 || !sel.toString().trim()) continue;
      copied = doc.execCommand("copy");
    }
    if (!copied) {
      copy(text);
    }
    this.props.handleOpenMenu(false);
    for (let i = 0; i < docs.length; i++) {
      let doc = docs[i];
      if (!doc) continue;
      doc.getSelection()?.empty();
    }
    toast.success(this.props.t("Copying successful"));
  };

  //翻译
  handleTrans = () => {
    this.props.handleMenuMode("trans");
    this.props.handleOriginalText(getSelection(this.props.currentBook.format));
    this.props.handleOpenMenu(true);
  };


  //词典
  handleDict = () => {
    this.props.handleMenuMode("dict");
    this.props.handleOriginalText(getSelection(this.props.currentBook.format));
    this.props.handleOriginalSentence(
      getSelectionSentence(this.props.currentBook.format)
    );
    this.props.handleOpenMenu(true);
  };


  //高亮
  handleDigest = async () => {
    await createHighlight({
      currentBook: this.props.currentBook,
      htmlBook: this.props.htmlBook,
      chapterDocIndex: this.props.chapterDocIndex,
      chapter: this.props.chapter,
      color: this.highlightUtil.formatHighlightValue(this.props.highlight),
      t: this.props.t,
      onNoteClick: this.handleNoteClick,
      onSuccess: () => {
        this.props.handleOpenMenu(false);
        this.props.handleFetchNotes();
        this.props.handleMenuMode("");
      },
    });
  };


  //点击高亮部分弹出笔记窗口
  handleNoteClick = (event: Event) => {
    this.props.handleNoteKey((event.target as any).dataset.key);
    this.props.handleMenuMode("note");
    this.props.handleOpenMenu(true);
  };


  //工具方法
  handleJump = (url: string) => {
    openExternalUrl(url);
  };


  //联网搜索
  handleSearchInternet = () => {
    switch (ConfigService.getReaderConfig("searchEngine")) {
      case "google":
        this.handleJump(
          "https://www.google.com/search?q=" +
            getSelection(this.props.currentBook.format)
        );
        break;
      case "baidu":
        this.handleJump(
          "https://www.baidu.com/s?wd=" +
            getSelection(this.props.currentBook.format)
        );
        break;
      case "bing":
        this.handleJump(
          "https://www.bing.com/search?q=" +
            getSelection(this.props.currentBook.format)
        );
        break;
      case "duckduckgo":
        this.handleJump(
          "https://duckduckgo.com/?q=" +
            getSelection(this.props.currentBook.format)
        );
        break;
      case "yandex":
        this.handleJump(
          "https://yandex.com/search/?text=" +
            getSelection(this.props.currentBook.format)
        );
        break;
      case "yahoo":
        this.handleJump(
          "https://search.yahoo.com/search?p=" +
            getSelection(this.props.currentBook.format)
        );
        break;
      case "naver":
        this.handleJump(
          "https://search.naver.com/search.naver?where=nexearch&sm=top_hty&fbm=1&ie=utf8&query=" +
            getSelection(this.props.currentBook.format)
        );
        break;
      case "baike":
        this.handleJump(
          "https://baike.baidu.com/item/" +
            getSelection(this.props.currentBook.format)
        );
        break;
      case "wiki":
        this.handleJump(
          "https://en.wikipedia.org/wiki/" +
            getSelection(this.props.currentBook.format)
        );
        break;
      default:
        this.handleJump(
          navigator.language === "zh-CN"
            ? "https://www.baidu.com/s?wd=" +
                getSelection(this.props.currentBook.format)
            : "https://www.google.com/search?q=" +
                getSelection(this.props.currentBook.format)
        );
        break;
    }
  };


  //书内检索
  handleSearchBook = () => {
    searchInTheBook("", this.props.currentBook.format, true);
    this.props.handleOpenMenu(false);
  };


  //朗读选中文字
  handleSpeak = () => {
    var msg = new SpeechSynthesisUtterance();
    msg.text = getSelection(this.props.currentBook.format);
    if (window.speechSynthesis && window.speechSynthesis.getVoices) {
      msg.voice = window.speechSynthesis.getVoices()[0];
      window.speechSynthesis.speak(msg);
    }
  };

  //从选中部分开始朗读文字
  handleReadFromHere = () => {
    const text =
      getSelectionSentence(this.props.currentBook.format) ||
      getSelection(this.props.currentBook.format);
    if (!text) return;

    this.props.handleSpeechStartText(text);
    this.props.handleSpeechAutoStart(true);
    this.props.handleSpeechDialog(true);
    this.props.handleOpenMenu(false);
  };


  //ai助手键
  handleAssistant = () => {
    const text = getSelection(this.props.currentBook.format);//拿到当前划选的文本
    if (!text) return;
    this.props.handleQuoteText(text);//把选中文字传给父组件，保存待传给 Agent 的文本
    this.props.handleMenuMode("assistant");//切换菜单模式为 assistant，页面会渲染 AI 助手面板
    this.props.handleOpenMenu(true);//保持弹窗打开，展示助手界面
    //为什么 Ctrl + 点击 点不动这几个 props 函数
    // handleQuoteText、handleMenuMode、handleOpenMenu 不是在当前 PopupMenu 文件定义的函数，是上层父组件通过 props 传下来的回调
  };

  //自定义菜单
  handleOpenPopupOptionDialog = () => {
    this.props.handleOpenMenu(false);
    this.props.handlePopupOptionDialog(true);
  };


  //总分发入口
  //所有 handle 方法，**不直接改页面状态**，而是调用 `this.props.xxx()`
  //props 是父组件传过来的一堆回调函数，子组件调用回调，**通知父组件改变状态**（打开弹窗、传文本、关闭菜单）

  handleOptionClick = (optionKey: PopupOptionKey) => {
    switch (optionKey) {
      case "note":
        this.handleNote();
        break;
      case "highlight":
        this.handleDigest();
        break;
      case "translation":
        this.handleTrans();
        break;
      case "copy":
        this.handleCopy();
        break;
      case "search-book":
        this.handleSearchBook();
        break;
      case "dict":
        this.handleDict();
        break;
      case "browser":
        this.handleSearchInternet();
        break;
      case "speaker":
        this.handleSpeak();
        break;
      case "speech-start":
        this.handleReadFromHere();
        break;
      case "assistant":
        this.handleAssistant();
        break;
      default:
        break;
    }
  };

  //**返回页面 UI 结构**，用来渲染菜单 DOM
  //先过滤菜单列表：读取配置，如果用户关闭 AI，就把`assistant`AI 按钮隐藏
  render() {
    const popupOptionKeys = getEnabledPopupOptionKeys().filter((item) => {
      return !(
        item === "assistant" &&
        ConfigService.getReaderConfig("isDisableAI") === "yes"
      );
    });
    return (
      <div className="menu-list">
        <Tooltip id="option-tooltip" style={{ zIndex: 25 }} />
        {popupOptionKeys.map((itemKey) => {
          const item = popupOptionMap[itemKey];
          return (
            <div
              key={item.key}
              className={item.name + "-option"}
              onClick={() => {
                this.handleOptionClick(item.key);
              }}
            >
              <span
                data-tooltip-id="option-tooltip"
                data-tooltip-content={this.props.t(item.title)}
              >
                <span
                  className={`icon-${item.icon} ${item.name}-icon`}
                  style={{ pointerEvents: "none" }}
                ></span>
              </span>
            </div>
          );
        })}
        <div
          className="setting-option"
          onClick={() => {
            this.handleOpenPopupOptionDialog();
          }}
        >
          <span
            data-tooltip-id="option-tooltip"
            data-tooltip-content={this.props.t("Customize popup menu")}
          >
            <span
              className="icon-setting setting-icon"
              style={{ color: "#8a8f9f", fontSize: "20px" }}
            ></span>
          </span>
        </div>
      </div>
    );
  }
}

//把这个组件导出，别的文件可以 import 引入这个组件。
// 类比 Java：把这个类`public`，其他包可以 new 这个类；但是前端不是 new，是直接当成 UI 标签使用。
export default PopupOption;
