package local.suspendedclock;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Original core navigation and wording, with a separate system-aware visual theme. */
public final class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayDeque<String> back = new ArrayDeque<>();
    private final List<ClockCardView> previews = new ArrayList<>();
    private ClockPrefs prefs;
    private UiTheme theme;
    private ClockAlerts alerts;
    private LinearLayout root, content;
    private ClockFaceView face;
    private Button start;
    private TextView colorWarning;
    private String page = "home";
    private boolean pendingFloating;
    private Dialog activeSheet;
    private boolean sorting;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            TimerStore.finish(MainActivity.this);
            if (!ClockService.running && !PipActivity.running) alerts.update(TimerStore.read(MainActivity.this));
            if (face != null) face.update();
            if (start != null) {
                String text = prefs.text("floatingMode", "pip").equals("pip") ? "开启画中画" : ClockService.running ? "关闭悬浮窗" : "开启悬浮窗";
                if (!text.contentEquals(start.getText())) start.setText(text);
            }
            for (ClockCardView preview : previews) preview.update();
            handler.postDelayed(this, prefs.interval());
        }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = new ClockPrefs(this); alerts=new ClockAlerts(this,prefs); theme = new UiTheme(this); ClockNotifications.channels(this);
        if (saved != null) { page=saved.getString("page","home");pendingFloating=saved.getBoolean("pending");String[] history=saved.getStringArray("back");if(history!=null)back.addAll(Arrays.asList(history)); }
        if (prefs.number("uiVersion",0)<2) {
            prefs.values.edit().putInt("uiVersion",2).putString("single","显示操作按钮").putString("double","缩放悬浮窗").apply();
        }
        if(prefs.number("appearanceDefaults",0)<3){prefs.resetColors();prefs.set("appearanceDefaults",3);}
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::navigateBack);
        render();
    }
    private void render() {
        face=null;start=null;colorWarning=null;previews.clear();
        root=column();root.setBackgroundColor(theme.background);setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());root.setPadding(safe.left,safe.top,safe.right,safe.bottom);}
            else root.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });root.requestApplyInsets();
        if(Build.VERSION.SDK_INT==26)getWindow().setNavigationBarColor(0xff1c1c1e);
        else if(Build.VERSION.SDK_INT<30)getWindow().getDecorView().setSystemUiVisibility(theme.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(Build.VERSION.SDK_INT>=30){android.view.WindowInsetsController controller=getWindow().getInsetsController();if(controller!=null)controller.setSystemBarsAppearance(theme.dark?0:android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);}
        if(page.equals("home")){home();return;}
        root.addView(toolbar(title(page),true));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);content=column();content.setPadding(dp(20),dp(20),dp(20),dp(28));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        switch(page){case "settings":settings();break;case "clock":clockSettings();break;case "palette":palette();break;case "font":fonts();break;case "style":styles();break;case "source":source();break;case "modes":modes();break;case "progress":progress();break;}
    }
    private String title(String p){switch(p){case"settings":return"悬浮窗设置";case"clock":return"时钟";case"palette":return"调色板";case"font":return"字体调整";case"style":return"悬浮窗样式";case"source":return"时间源";case"modes":return"显示模式管理";case"progress":return"时间进度条";default:return"悬浮时钟";}}
    private void go(String next){if(activeSheet!=null)activeSheet.dismiss();back.push(page);page=next;render();}
    private void navigateBack(){if(activeSheet!=null&&activeSheet.isShowing()){activeSheet.dismiss();return;}if(!back.isEmpty()){page=back.pop();render();}else finish();}
    @Override public boolean onKeyUp(int code,android.view.KeyEvent event){if(code==android.view.KeyEvent.KEYCODE_BACK&&Build.VERSION.SDK_INT<33){navigateBack();return true;}return super.onKeyUp(code,event);}
    private View toolbar(String title,boolean backButton){
        FrameLayout bar=new FrameLayout(this);bar.setMinimumHeight(dp(52));
        TextView label=text(title,backButton?18:22,theme.text);label.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));label.setGravity(backButton?Gravity.CENTER:Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,dp(52));lp.leftMargin=dp(backButton?52:20);lp.rightMargin=dp(52);bar.addView(label,lp);
        IconView icon=new IconView(this,backButton?"back":"settings",backButton?theme.blue:theme.text);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);icon.setContentDescription(backButton?"返回":"悬浮窗设置");icon.setPadding(dp(12),dp(12),dp(12),dp(12));icon.setBackground(UiTheme.press(0,dp(24)));icon.setOnClickListener(v->{if(backButton)navigateBack();else go("settings");});
        FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(48),dp(48),backButton?Gravity.START|Gravity.CENTER_VERTICAL:Gravity.END|Gravity.CENTER_VERTICAL);bar.addView(icon,ip);return bar;
    }
    private void home(){
        boolean large=getResources().getConfiguration().fontScale>1.3f;
        LinearLayout body=root;
        if(large){ScrollView scroll=new ScrollView(this);body=column();scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));}
        body.addView(toolbar("悬浮时钟",false));
        FrameLayout holder=new FrameLayout(this);face=new ClockFaceView(this,prefs,theme,this::countdownPicker,()->TimerStore.toggle(this));holder.addView(face,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));body.addView(holder,large?new LinearLayout.LayoutParams(-1,dp(340)):new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=column();footer.setPadding(dp(20),0,dp(20),dp(16));
        TextView notice=text("建议设置后台运行无限制，防止悬浮窗消失",12,theme.secondary);notice.setPadding(dp(12),dp(10),dp(12),dp(10));notice.setBackground(UiTheme.press(theme.surface,dp(12)));notice.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));footer.addView(notice);
        add(footer,controlRow(false),14);
        IconView expand=new IconView(this,"up",theme.secondary);expand.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);expand.setContentDescription("时钟控制");expand.setPadding(dp(10),dp(8),dp(10),dp(8));expand.setOnClickListener(v->controls());footer.addView(expand,new LinearLayout.LayoutParams(-1,dp(36)));
        start=button("开启悬浮窗",true,()->{
            if(prefs.text("floatingMode","pip").equals("pip")){stopService(new Intent(this,ClockService.class));startActivity(new Intent(this,PipActivity.class));}
            else if(ClockService.running)stopService(new Intent(this,ClockService.class));else requestFloating();
        });footer.addView(start,new LinearLayout.LayoutParams(-1,dp(Math.max(50,Math.round(24*getResources().getConfiguration().fontScale+20)))));body.addView(footer);
    }
    private String[] order(){String[] value=prefs.text("controlsOrder","浮窗模式,倒计时,时间源,切换模式").split(",");return value.length==4?value:new String[]{"浮窗模式","倒计时","时间源","切换模式"};}
    private View controlRow(boolean inSheet){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.TOP);
        String[] names=order();for(int i=0;i<names.length;i++){final String name=names[i];String icon=name.equals("浮窗模式")?"float":name.equals("倒计时")?"countdown":name.equals("时间源")?"source":"switch";
            View item=control(name,icon,()->controlClick(name));item.setOnLongClickListener(v->{if(sorting&&inSheet){v.startDragAndDrop(ClipData.newPlainText("control",name),new View.DragShadowBuilder(v),name,0);}else controlMenu(name);return true;});
            item.setOnDragListener((v,event)->{if(event.getAction()==DragEvent.ACTION_DROP&&sorting){String from=(String)event.getLocalState();List<String> list=new ArrayList<>(Arrays.asList(order()));int a=list.indexOf(from),b=list.indexOf(name);if(a>=0&&b>=0){java.util.Collections.swap(list,a,b);prefs.set("controlsOrder",String.join(",",list));controls();}return true;}return event.getAction()==DragEvent.ACTION_DRAG_STARTED;});
            row.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        }return row;
    }
    private View control(String label,String icon,Runnable action){
        LinearLayout col=column();col.setGravity(Gravity.CENTER);col.setPadding(dp(2),dp(8),dp(2),dp(6));col.setContentDescription(label);col.setBackground(UiTheme.press(0,dp(14)));col.setOnClickListener(v->action.run());
        IconView image=new IconView(this,icon,theme.blue);image.setPadding(dp(15),dp(15),dp(15),dp(15));image.setBackground(UiTheme.shape(theme.surface,dp(32)));col.addView(image,new LinearLayout.LayoutParams(dp(60),dp(60)));
        TextView txt=text(label,12,theme.text);txt.setGravity(Gravity.CENTER);txt.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(8);col.addView(txt,tp);return col;
    }
    private void controlClick(String name){switch(name){case"浮窗模式":floatingMode();break;case"倒计时":countdownPicker();break;case"时间源":go("source");break;default:selectMode();}}
    private void selectMode(){
        List<String> modes=new ArrayList<>();if(prefs.bool("enableClock",true))modes.add("时钟");if(prefs.bool("enableCountdown",true))modes.add("倒计时");
        choices("显示模式",modes.toArray(new String[0]),modes.indexOf(prefs.countdown()?"倒计时":"时钟"),index->{prefs.set("mode",modes.get(index).equals("时钟")?"clock":"countdown");if(activeSheet!=null)activeSheet.dismiss();if(face!=null)face.update();});
    }
    private void controls(){
        if(activeSheet!=null)activeSheet.dismiss();LinearLayout panel=sheetPanel();
        LinearLayout heading=new LinearLayout(this);TextView title=text("时钟控制",17,theme.text);title.setTypeface(Typeface.DEFAULT_BOLD);title.setGravity(Gravity.CENTER);heading.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));Button sort=button(sorting?"完成":"排序",false,()->{sorting=!sorting;controls();});heading.addView(sort,new LinearLayout.LayoutParams(dp(68),dp(44)));panel.addView(heading);
        TextView hint=text(sorting?"长按拖拽调整顺序":"【长按图标】进行更多设置",12,theme.secondary);hint.setGravity(Gravity.CENTER);panel.addView(hint);add(panel,controlRow(true),12);divider(panel);
        LinearLayout more=new LinearLayout(this);more.addView(new View(this),new LinearLayout.LayoutParams(0,1,2));more.addView(control("更多设置","settings",()->go("clock")),new LinearLayout.LayoutParams(0,-2,1));more.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));add(panel,more,8);
        if(sorting)add(panel,button("恢复默认",false,()->{prefs.set("controlsOrder","浮窗模式,倒计时,时间源,切换模式");controls();}),8);
        showSheet(panel);
    }
    private void controlMenu(String name){
        if(name.equals("倒计时")){
            new AlertDialog.Builder(this).setTitle("倒计时").setItems(new String[]{"倒计时结束后显示","设定倒计时时间","重新开始"},(d,i)->{
                if(i==0)choices("倒计时结束后显示",new String[]{"当前时间","00:00:00","正计时"},endIndex(),x->prefs.set("end",new String[]{"clock","zero","countup"}[x]));
                else if(i==1)countdownPicker();else {Countdown state=TimerStore.read(this);if(state.target&&state.targetWall<=System.currentTimeMillis())countdownPicker();else TimerStore.restart(this);}
            }).show();
        }else if(name.equals("时间源")){new AlertDialog.Builder(this).setTitle("时间源").setItems(new String[]{"校准时间源"},(d,i)->toast("设备时间无需校准")).show();}
        else if(name.equals("浮窗模式"))go("settings");else go("modes");
    }
    private int endIndex(){return Arrays.asList("clock","zero","countup").indexOf(prefs.text("end","zero"));}
    private void floatingMode(){
        LinearLayout panel=sheetPanel();panel.addView(text("模式选择",20,theme.text));LinearLayout cards=new LinearLayout(this);
        String[][] copy={{"画中画","可在任意App上显示","仅支持半透明","样式、功能较少"},{"悬浮窗","支持全透明","样式、功能丰富","在部分App上无法显示"}};
        for(int i=0;i<2;i++){final int index=i;LinearLayout card=column();card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackground(UiTheme.press(theme.fill,dp(18)));TextView title=text(copy[i][0],19,theme.text);title.setTypeface(Typeface.DEFAULT_BOLD);card.addView(title);for(int j=1;j<4;j++)add(card,text(copy[i][j],13,theme.secondary),12);card.setOnClickListener(v->{prefs.set("floatingMode",index==0?"pip":"overlay");if(index==0&&prefs.style()>3)prefs.set("windowStyle",0);if(activeSheet!=null)activeSheet.dismiss();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(4),dp(16),dp(4),dp(4));cards.addView(card,lp);}panel.addView(cards);showSheet(panel);
    }
    private void countdownPicker(){
        if(activeSheet!=null)activeSheet.dismiss();Countdown state=TimerStore.read(this);
        LinearLayout panel=sheetPanel(),tabs=new LinearLayout(this);Button target=button("目标时间",false,()->{}),duration=button("时长",false,()->{}),date=button("日期: "+LocalDate.now(),false,()->{});
        tabs.addView(target,new LinearLayout.LayoutParams(0,dp(48),1));tabs.addView(duration,new LinearLayout.LayoutParams(0,dp(48),.7f));tabs.addView(date,new LinearLayout.LayoutParams(0,dp(48),1.5f));panel.addView(tabs);
        final boolean[] useTarget={prefs.text("pickerType","target").equals("target")};final LocalDate[] picked={state.target&&state.targetWall>System.currentTimeMillis()?java.time.Instant.ofEpochMilli(state.targetWall).atZone(ZoneId.systemDefault()).toLocalDate():LocalDateTime.now().plusHours(1).toLocalDate()};
        LinearLayout wheels=new LinearLayout(this);wheels.setGravity(Gravity.CENTER);
        NumberPicker hour=wheel(0,23,"时"),minute=wheel(0,59,"分"),second=wheel(0,59,"秒");NumberPicker[] all={hour,minute,second};TextView[] units=new TextView[3];
        for(int index=0;index<all.length;index++){LinearLayout column=new LinearLayout(this);column.setGravity(Gravity.CENTER_VERTICAL);column.addView(all[index],new LinearLayout.LayoutParams(0,dp(220),1));TextView unit=text(new String[]{"时","分","秒"}[index],14,theme.secondary);units[index]=unit;column.addView(unit);wheels.addView(column,new LinearLayout.LayoutParams(0,dp(220),1));}add(panel,wheels,12);
        Runnable refresh=()->{units[0].setText(useTarget[0]?"时":"小时");target.setTextColor(useTarget[0]?theme.blue:theme.secondary);duration.setTextColor(useTarget[0]?theme.secondary:theme.blue);date.setVisibility(useTarget[0]?View.VISIBLE:View.INVISIBLE);date.setText("日期: "+picked[0]);
            if(useTarget[0]){LocalTime t=state.target&&state.targetWall>System.currentTimeMillis()?java.time.Instant.ofEpochMilli(state.targetWall).atZone(ZoneId.systemDefault()).toLocalTime():LocalTime.now().plusHours(1).withMinute(0).withSecond(0);hour.setValue(t.getHour());minute.setValue(t.getMinute());second.setValue(t.getSecond());}
            else{long s=state.duration/1000;hour.setValue((int)(s/3600%24));minute.setValue((int)(s/60%60));second.setValue((int)(s%60));}};
        target.setOnClickListener(v->{useTarget[0]=true;refresh.run();});duration.setOnClickListener(v->{useTarget[0]=false;refresh.run();});date.setOnClickListener(v->new DatePickerDialog(this,(p,y,m,d)->{picked[0]=LocalDate.of(y,m+1,d);date.setText("日期: "+picked[0]);},picked[0].getYear(),picked[0].getMonthValue()-1,picked[0].getDayOfMonth()).show());refresh.run();
        LinearLayout actions=new LinearLayout(this);Button cancel=button("取消",false,()->{if(activeSheet!=null)activeSheet.dismiss();});Button confirm=button("确定",true,()->{
            for(NumberPicker picker:all)picker.clearFocus();long seconds=hour.getValue()*3600L+minute.getValue()*60L+second.getValue();
            if(useTarget[0]){long wall=LocalDateTime.of(picked[0],LocalTime.of(hour.getValue(),minute.getValue(),second.getValue())).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();if(wall<=System.currentTimeMillis()){new AlertDialog.Builder(this).setTitle("温馨提示").setMessage("您选择的时间已过期，是否修改？").setPositiveButton("修改",null).setNegativeButton("继续",(d,i)->{TimerStore.startTarget(this,wall);finishCountdownPicker(true);}).show();return;}TimerStore.startTarget(this,wall);}
            else{if(seconds==0){toast("倒计时不可设置为0秒");return;}TimerStore.start(this,seconds*1000);}
            finishCountdownPicker(useTarget[0]);
        });weighted(actions,cancel);weighted(actions,confirm);add(panel,actions,12);showSheet(panel);
    }
    private void finishCountdownPicker(boolean target){prefs.set("enableCountdown",true);prefs.set("mode","countdown");prefs.set("pickerType",target?"target":"duration");if(activeSheet!=null)activeSheet.dismiss();if(face!=null)face.update();requestReminderPermissions();}
    private NumberPicker wheel(int min,int max,String label){NumberPicker picker=new NumberPicker(this);picker.setMinValue(min);picker.setMaxValue(max);picker.setWrapSelectorWheel(true);picker.setFormatter(v->String.format(Locale.ROOT,"%02d",v));picker.setContentDescription(label);return picker;}
    private void settings(){
        LinearLayout group=group();nav(group,"悬浮窗样式","style","style");nav(group,"显示模式管理","switch","modes");nav(group,"调色板","palette","palette");nav(group,"字体调整","font","font");
        group=group();nav(group,"时钟 (当前模式)","clock","clock");
        group=group();select(group,"单击悬浮窗","single","显示操作按钮",new String[]{"显示操作按钮","缩放悬浮窗","回到APP并关闭悬浮窗","切换显示模式","暂停/开始计时","重置计时","关闭悬浮窗","无操作"});
        select(group,"双击悬浮窗","double","缩放悬浮窗",new String[]{"显示操作按钮","缩放悬浮窗","回到APP并关闭悬浮窗","切换显示模式","暂停/开始计时","重置计时","关闭悬浮窗","无操作"});
        toggle(group,"避让系统顶栏","avoidBars",true);
        group=group();toggle(group,"锁定悬浮窗并穿透点击","locked",false);toggle(group,"保持屏幕常亮","keepScreen",false);
        navAction(group,"通知设置","settings",()->startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName())));
        navAction(group,"闹钟和提醒","clock",this::exactPermission);
    }
    private void clockSettings(){
        preview();LinearLayout group=group();String[] keys={"topLeft","topRight","bottomLeft","bottomRight"},labels={"左上","右上","左下","右下"},defaults={"不显示","时间源","屏幕刷新率","不显示"};
        for(int i=0;i<4;i++)select(group,labels[i],keys[i],defaults[i],new String[]{"不显示","时间源","日期","屏幕刷新率","电量"});
        group=group();navValue(group,"时间源","设备时间",()->go("source"));
        String[] formats={"时:分:秒.毫秒","时:分:秒","时:分","分:秒.毫秒","分:秒","秒.毫秒","秒"};
        TextView format=valueRow(group,"时间格式",formats[Math.max(0,Arrays.asList(TimeText.FORMATS).indexOf(prefs.format()))]);
        ((View)format.getParent()).setOnClickListener(v->choices("时间格式",formats,Arrays.asList(TimeText.FORMATS).indexOf(prefs.format()),i->{prefs.set("format",TimeText.FORMATS[i]);format.setText(formats[i]);refreshPreviews();}));
        toggle(group,"秒针音效","secondTick",false);TextView watermark=valueRow(group,"自定义水印",prefs.text("watermark","").isEmpty()?"不显示":prefs.text("watermark",""));((View)watermark.getParent()).setOnClickListener(v->editText("自定义水印",prefs.text("watermark",""),s->{prefs.set("watermark",s);watermark.setText(s.isEmpty()?"不显示":s);refreshPreviews();}));
        group=group();navValue(group,"进度条样式",prefs.text("progress","点位标记"),()->go("progress"));select(group,"高亮提醒 (最后5s)","flash","关闭",new String[]{"关闭","开启"});select(group,"震动提醒 (最后3s)","vibrate","关闭",new String[]{"关闭","每秒","每半秒"});select(group,"倒计时播报 (最后5s)","voice","关闭",new String[]{"关闭","开启"});
        restore(()->{prefs.values.edit().remove("format").remove("topLeft").remove("topRight").remove("bottomLeft").remove("bottomRight").remove("secondTick").remove("watermark").remove("progress").remove("flash").remove("vibrate").remove("voice").apply();render();});
    }
    private void source(){
        LinearLayout group=group();TextView title=text("设备当前时间",17,theme.text);title.setPadding(dp(16),dp(20),dp(16),dp(20));group.addView(title);
        navValue(group,"时间源","设备时间",()->toast("设备时间无需校准"));
        add(content,text("离线版本使用设备时间",13,theme.secondary),16);
        add(content,button("校准时间源",false,()->toast("设备时间无需校准")),24);
    }
    private void modes(){
        LinearLayout group=group();for(String name:new String[]{"时钟","倒计时"}){String key=name.equals("时钟")?"enableClock":"enableCountdown";Switch control=toggle(group,name,key,true);control.setOnCheckedChangeListener((b,checked)->{if(!checked&&!prefs.bool(name.equals("时钟")?"enableCountdown":"enableClock",true)){control.setChecked(true);toast("至少保留一个显示模式");return;}prefs.set(key,checked);if(!checked&&prefs.countdown()==name.equals("倒计时"))prefs.set("mode",name.equals("时钟")?"countdown":"clock");});}
    }
    private void progress(){preview();LinearLayout group=group();select(group,"进度条样式","progress","点位标记",new String[]{"点位标记","顶部","底部","刻度尺","简约刻度","时间点","不显示"});select(group,"显示时机","progressWhen","始终显示",new String[]{"始终显示","倒计时"});}
    private void preview(){
        ClockCardView view=new ClockCardView(this,prefs,true);previews.add(view);FrameLayout box=new FrameLayout(this);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(Math.min(240,prefs.width())),dp(prefs.height()),Gravity.CENTER);box.addView(view,lp);content.addView(box,new LinearLayout.LayoutParams(-1,dp(Math.max(150,prefs.height()+24))));
    }
    private static final int[][] PALETTES={{0xb3475164,0xffffffff,0xffff5d42},{0x00ffffff,0xff333333,0xffff5a3c},{0xff485062,0xffffffff,0xffff5a3c},{0xffffffff,0xff333333,0xffff5a3c},{0xff333333,0xffffffff,0xffff5a3c},{0xffff5a32,0xffffffff,0xffffe94b},{0xff5374ff,0xffffffff,0xffffe94b},{0xff00ad99,0xffffffff,0xffffe94b},{0xff7b7599,0xffffffff,0xffffe94b},{0xffff9a76,0xffffffff,0xffffe94b},{0xff839da7,0xffffffff,0xffffe94b},{0xffa5e65f,0xff333333,0xffff5a3c}};
    private void palette(){
        preview();colorWarning=text("文字颜色和背景色过于相近，可能无法正常显示",12,0xffff453a);colorWarning.setGravity(Gravity.CENTER);add(content,colorWarning,4);refreshPreviews();LinearLayout tabs=new LinearLayout(this);Button solid=button("纯色背景",false,()->{prefs.set("imageBackground",false);refreshPreviews();}),image=button("图片背景",false,()->pickFile(false));weighted(tabs,solid);weighted(tabs,image);add(content,tabs,16);
        LinearLayout grid=group();grid.setPadding(dp(8),dp(8),dp(8),dp(8));
        for(int row=0;row<4;row++){LinearLayout line=new LinearLayout(this);for(int col=0;col<3;col++){final int index=row*3+col;int[] colors=PALETTES[index];FrameLayout sample=sample("12:34:56.7",colors[0],colors[1],colors[2],prefs.font());sample.setContentDescription("颜色方案 "+(index+1));sample.setOnClickListener(v->{prefs.values.edit().putInt("clockBackground",colors[0]|0xff000000).putInt("clockText",colors[1]).putInt("clockMilli",colors[2]).putInt("clockOpacity",Color.alpha(colors[0])*100/255).putBoolean("imageBackground",false).apply();render();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(58),1);lp.setMargins(dp(5),dp(8),dp(5),dp(8));line.addView(sample,lp);}grid.addView(line);}
        LinearLayout custom=group();TextView caption=text("自定义颜色",13,theme.secondary);caption.setPadding(dp(16),dp(12),dp(16),dp(8));custom.addView(caption);
        LinearLayout row=new LinearLayout(this);String[] labels={"背景","字体","毫秒"},keys={"clockBackground","clockText","clockMilli"};int[] defaults={0xff475164,0xffffffff,0xffff5d42};
        FrameLayout current=sample("12:34:56.7",prefs.background(),prefs.textColor(),prefs.milliColor(),prefs.font());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(52),1.4f);cp.setMargins(dp(12),dp(8),dp(8),dp(8));row.addView(current,cp);
        for(int i=0;i<3;i++){final int index=i;LinearLayout item=column();item.setGravity(Gravity.CENTER);item.setBackground(UiTheme.press(0,dp(12)));View dot=new View(this);dot.setBackground(UiTheme.shape(prefs.number(keys[i],defaults[i]),dp(12)));item.addView(dot,new LinearLayout.LayoutParams(dp(22),dp(22)));TextView label=text(labels[i],12,theme.secondary);label.setGravity(Gravity.CENTER);add(item,label,7);item.setOnClickListener(v->colorPicker(labels[index],keys[index],defaults[index]));row.addView(item,new LinearLayout.LayoutParams(0,dp(72),1));}custom.addView(row);slider(custom,"背景透明度","clockOpacity",0,100,70,"%");
        restore(()->{prefs.resetColors();render();});
    }
    private void colorPicker(String title,String key,int fallback){
        LinearLayout panel=sheetPanel();panel.addView(text(title,20,theme.text));EditText hex=new EditText(this);hex.setSingleLine(true);hex.setTextColor(theme.text);hex.setContentDescription("十六进制颜色");hex.setText(String.format(Locale.ROOT,"#%06X",prefs.number(key,fallback)&0xffffff));add(panel,hex,16);
        int[] rgb={Color.red(prefs.number(key,fallback)),Color.green(prefs.number(key,fallback)),Color.blue(prefs.number(key,fallback))};String[] labels={"R","G","B"};
        for(int i=0;i<3;i++){final int channel=i;TextView label=text(labels[i],13,theme.secondary);add(panel,label,8);SeekBar bar=new SeekBar(this);bar.setMax(255);bar.setProgress(rgb[i]);bar.setContentDescription(labels[i]);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int value,boolean user){if(user){rgb[channel]=value;hex.setText(String.format(Locale.ROOT,"#%02X%02X%02X",rgb[0],rgb[1],rgb[2]));}}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});panel.addView(bar,new LinearLayout.LayoutParams(-1,dp(48)));}
        LinearLayout actions=new LinearLayout(this);weighted(actions,button("取消",false,()->{if(activeSheet!=null)activeSheet.dismiss();}));weighted(actions,button("确定",true,()->{String value=hex.getText().toString().trim();if(!value.matches("#[0-9a-fA-F]{6}")){hex.setError("请输入 #RRGGBB");return;}prefs.set(key,Color.parseColor(value));if(activeSheet!=null)activeSheet.dismiss();render();}));add(panel,actions,12);showSheet(panel);
    }
    private void fonts(){
        preview();stepper("中间字体大小","scaleMain",50,200,5,100,"%");stepper("四周字体大小","scaleSub",50,200,5,100,"%");if(prefs.style()>3)stepper("字体纵向偏移","verticalOffset",-20,20,1,0,"");
        TextView title=text("选择字体",17,theme.text);title.setGravity(Gravity.CENTER);add(content,title,24);LinearLayout grid=group();grid.setPadding(dp(8),dp(8),dp(8),dp(8));
        for(int row=0;row<8;row++){LinearLayout line=new LinearLayout(this);for(int col=0;col<2;col++){final int index=row*2+col;FrameLayout sample=sample(index==15?"自定义 +":"12:34:56.7",prefs.number("fontIndex",0)==index?theme.blue:theme.surface,prefs.number("fontIndex",0)==index?Color.WHITE:theme.text,prefs.number("fontIndex",0)==index?Color.WHITE:theme.text,Typeface.create(ClockPrefs.FONTS[index].equals("custom")?"sans-serif":ClockPrefs.FONTS[index],index==14?Typeface.ITALIC:Typeface.NORMAL));sample.setContentDescription(index==15?"自定义字体":"字体选项 "+(index+1));sample.setOnClickListener(v->{if(index==15)pickFile(true);else{prefs.set("fontIndex",index);render();}});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(54),1);lp.setMargins(dp(4),dp(6),dp(4),dp(6));line.addView(sample,lp);}grid.addView(line);}
        restore(()->{prefs.resetFonts();render();});
    }
    private void styles(){
        preview();for(int row=0;row<3;row++){LinearLayout line=new LinearLayout(this);for(int col=0;col<2;col++){final int index=row*2+col;FrameLayout cell=new FrameLayout(this);cell.setBackground(UiTheme.press(prefs.style()==index?theme.fill:theme.surface,dp(20)));cell.setContentDescription("悬浮窗样式 "+(index+1));
            FrameLayout sample=sample("12:34:56.7",theme.blue,Color.WHITE,Color.WHITE,Typeface.create("sans-serif-medium",Typeface.NORMAL));float ratio=ClockPrefs.WIDTHS[index]/(float)ClockPrefs.HEIGHTS[index];int w=ratio<1.2?78:Math.min(130,100+(int)(ratio*8));int h=Math.round(w/ratio);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(w),dp(h),Gravity.CENTER);cell.addView(sample,lp);
            cell.setOnClickListener(v->{if(index>3&&prefs.text("floatingMode","pip").equals("pip")){toast("该样式仅支持悬浮窗模式");return;}prefs.set("windowStyle",index);render();});LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(132),1);cp.setMargins(dp(4),dp(12),dp(4),dp(12));line.addView(cell,cp);}content.addView(line);}
    }
    private FrameLayout sample(String value,int bg,int color,int milli,Typeface typeface){
        FrameLayout frame=new FrameLayout(this);android.graphics.drawable.GradientDrawable outline=UiTheme.shape(bg,dp(14));outline.setStroke(Math.max(1,dp(1)/2),theme.line);frame.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x227f7f7f),outline,UiTheme.shape(Color.WHITE,dp(14))));TextView text=new TextView(this);text.setGravity(Gravity.CENTER);text.setTextColor(color);text.setTypeface(typeface);text.setMaxLines(1);text.setPadding(dp(7),0,dp(7),0);text.setAutoSizeTextTypeUniformWithConfiguration(8,18,1,android.util.TypedValue.COMPLEX_UNIT_SP);android.text.SpannableString styled=new android.text.SpannableString(value);int point=value.indexOf('.');if(point>=0)styled.setSpan(new android.text.style.ForegroundColorSpan(milli),point+1,value.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);text.setText(styled);frame.addView(text,new FrameLayout.LayoutParams(-1,-1));return frame;
    }
    private void stepper(String label,String key,int min,int max,int step,int fallback,String suffix){
        LinearLayout group=group(),row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(16),dp(8),dp(12),dp(8));TextView name=text(label,14,theme.text);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));TextView value=text(prefs.number(key,fallback)+suffix,17,theme.text);value.setTypeface(Typeface.DEFAULT_BOLD);value.setGravity(Gravity.CENTER);row.addView(value,new LinearLayout.LayoutParams(dp(62),-2));for(int delta:new int[]{-step,step}){Button button=button(delta<0?"−":"+",false,()->{int next=Math.max(min,Math.min(max,prefs.number(key,fallback)+delta));prefs.set(key,next);value.setText(next+suffix);refreshPreviews();});button.setContentDescription((delta<0?"减小":"增大")+label);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(48),dp(44));lp.leftMargin=dp(6);row.addView(button,lp);}group.addView(row);
    }
    private void nav(LinearLayout parent,String label,String icon,String next){navAction(parent,label,icon,()->go(next));}
    private void navAction(LinearLayout parent,String label,String icon,Runnable action){
        if(parent.getChildCount()>0)divider(parent);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(14),dp(8),dp(14),dp(8));row.setMinimumHeight(dp(52));row.setBackground(UiTheme.press(0,0));IconView symbol=new IconView(this,icon,theme.secondary);row.addView(symbol,new LinearLayout.LayoutParams(dp(22),dp(22)));TextView name=text(label,16,theme.text);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(12);row.addView(name,lp);IconView arrow=new IconView(this,"next",theme.secondary);row.addView(arrow,new LinearLayout.LayoutParams(dp(16),dp(16)));row.setOnClickListener(v->action.run());parent.addView(row);
    }
    private TextView valueRow(LinearLayout parent,String label,String selected){
        if(parent.getChildCount()>0)divider(parent);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(16),dp(13),dp(16),dp(13));row.setMinimumHeight(dp(52));row.setBackground(UiTheme.press(0,0));TextView name=text(label,15,theme.text);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));TextView value=text(selected,14,theme.blue);value.setGravity(Gravity.END);value.setMaxLines(2);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(8);row.addView(value,lp);parent.addView(row);return value;
    }
    private void navValue(LinearLayout parent,String label,String selected,Runnable action){TextView value=valueRow(parent,label,selected);((View)value.getParent()).setOnClickListener(v->action.run());}
    private void select(LinearLayout parent,String label,String key,String fallback,String[] options){TextView value=valueRow(parent,label,prefs.text(key,fallback));((View)value.getParent()).setOnClickListener(v->choices(label,options,Arrays.asList(options).indexOf(prefs.text(key,fallback)),i->{prefs.set(key,options[i]);value.setText(options[i]);refreshPreviews();}));}
    private Switch toggle(LinearLayout parent,String label,String key,boolean fallback){
        if(parent.getChildCount()>0)divider(parent);Switch control=new Switch(this);control.setText(label);control.setTextSize(15);control.setTextColor(theme.text);control.setPadding(dp(16),dp(10),dp(16),dp(10));control.setMinHeight(dp(54));control.setChecked(prefs.bool(key,fallback));control.setOnCheckedChangeListener((button,checked)->{prefs.set(key,checked);refreshPreviews();});parent.addView(control,new LinearLayout.LayoutParams(-1,-2));return control;
    }
    private void slider(LinearLayout parent,String label,String key,int min,int max,int fallback,String suffix){TextView name=text(label,14,theme.secondary);name.setPadding(dp(16),dp(12),dp(16),0);parent.addView(name);SeekBar bar=new SeekBar(this);bar.setContentDescription(label);bar.setMax(max-min);bar.setProgress(prefs.number(key,fallback)-min);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int value,boolean user){if(user){prefs.set(key,value+min);name.setText(String.format(Locale.CHINA,"%s %d%s",label,value+min,suffix));refreshPreviews();}}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});parent.addView(bar,new LinearLayout.LayoutParams(-1,dp(48)));}
    private void choices(String title,String[] labels,int selected,java.util.function.IntConsumer accept){AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels,Math.max(0,selected),(d,i)->{accept.accept(i);d.dismiss();}).setNegativeButton("取消",null).create();dialog.show();}
    private void editText(String title,String initial,java.util.function.Consumer<String> accept){EditText input=new EditText(this);input.setText(initial);input.setContentDescription(title);input.setTextColor(theme.text);input.setSingleLine(true);new AlertDialog.Builder(this).setTitle(title).setView(input).setPositiveButton("确定",(d,i)->accept.accept(input.getText().toString().trim())).setNegativeButton("取消",null).show();}
    private LinearLayout column(){LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);return layout;}
    private LinearLayout group(){LinearLayout group=column();group.setBackground(UiTheme.shape(theme.surface,dp(18)));add(content,group,16);return group;}
    private void divider(LinearLayout parent){View line=new View(this);line.setBackgroundColor(theme.line);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,Math.max(1,dp(1)/2));lp.leftMargin=dp(16);parent.addView(line,lp);}
    private TextView text(String value,int size,int color){TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);view.setLineSpacing(dp(2),1);return view;}
    private Button button(String label,boolean primary,Runnable action){Button view=new Button(this);view.setText(label);view.setTextSize(16);view.setAllCaps(false);view.setTextColor(primary?Color.WHITE:theme.blue);view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));view.setMinHeight(dp(48));view.setStateListAnimator(null);view.setElevation(0);view.setPadding(dp(10),dp(8),dp(10),dp(8));view.setBackground(UiTheme.press(primary?theme.blue:theme.fill,dp(14)));view.setOnClickListener(v->action.run());return view;}
    private void add(LinearLayout parent,View child,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);parent.addView(child,lp);}
    private void weighted(LinearLayout parent,View child){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(4),0,dp(4),0);parent.addView(child,lp);}
    private LinearLayout sheetPanel(){LinearLayout panel=column();panel.setPadding(dp(20),dp(20),dp(20),dp(24));panel.setBackground(UiTheme.shape(theme.surface,dp(24)));return panel;}
    private void showSheet(LinearLayout panel){if(activeSheet!=null)activeSheet.dismiss();Dialog dialog=new Dialog(this);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);dialog.setContentView(panel);Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setDimAmount(.3f);}dialog.show();if(window!=null){window.setGravity(Gravity.BOTTOM);window.setLayout(getResources().getDisplayMetrics().widthPixels-dp(24),-2);}activeSheet=dialog;}
    private void restore(Runnable action){add(content,button("恢复默认",false,()->new AlertDialog.Builder(this).setMessage("确定要恢复默认设置吗？").setPositiveButton("确定",(d,i)->action.run()).setNegativeButton("取消",null).show()),24);}
    private void refreshPreviews(){for(ClockCardView view:previews)view.refresh();if(face!=null)face.update();if(colorWarning!=null){int a=prefs.textColor(),b=prefs.background();int distance=Math.abs(Color.red(a)-Color.red(b))+Math.abs(Color.green(a)-Color.green(b))+Math.abs(Color.blue(a)-Color.blue(b));colorWarning.setVisibility(distance<100?View.VISIBLE:View.INVISIBLE);}}
    private void requestFloating(){if(!Settings.canDrawOverlays(this)){pendingFloating=true;try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(android.content.ActivityNotFoundException e){pendingFloating=false;toast("请在系统设置中允许悬浮窗权限");}return;}requestReminderPermissions();try{startForegroundService(new Intent(this,ClockService.class));}catch(RuntimeException e){toast("开启失败，请回到应用后重试");}}
    private void requestReminderPermissions(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);if(!TimerAlarm.available(this))toast("允许闹钟和提醒后可在锁屏准时提醒");}
    private void exactPermission(){if(Build.VERSION.SDK_INT>=31&&!TimerAlarm.available(this)){try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(android.content.ActivityNotFoundException e){toast("请在系统设置中允许闹钟和提醒");}}else toast("已允许闹钟和提醒");}
    private void pickFile(boolean font){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(font?"*/*":"image/*");startActivityForResult(intent,font?21:22);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null||(request!=21&&request!=22))return;java.io.File temp=null;try{
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();try(java.io.InputStream input=getContentResolver().openInputStream(data.getData())){if(input==null)throw new java.io.IOException();byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(bytes.size()+count>10*1024*1024)throw new java.io.IOException("文件超过10MB");bytes.write(buffer,0,count);}}
        temp=java.io.File.createTempFile("clock-import-",".tmp",getFilesDir());
        if(request==21){try(java.io.FileOutputStream output=new java.io.FileOutputStream(temp)){output.write(bytes.toByteArray());}Typeface.createFromFile(temp);java.nio.file.Files.move(temp.toPath(),new java.io.File(getFilesDir(),"clock-font.ttf").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);prefs.set("fontIndex",15);}
        else{byte[] raw=bytes.toByteArray();android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeByteArray(raw,0,raw.length,options);options.inSampleSize=1;while(options.outWidth/options.inSampleSize>2048||options.outHeight/options.inSampleSize>2048)options.inSampleSize*=2;options.inJustDecodeBounds=false;android.graphics.Bitmap image=android.graphics.BitmapFactory.decodeByteArray(raw,0,raw.length,options);if(image==null)throw new java.io.IOException("图片无法读取");try(java.io.FileOutputStream output=new java.io.FileOutputStream(temp)){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}image.recycle();java.nio.file.Files.move(temp.toPath(),new java.io.File(getFilesDir(),"clock-background.png").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);prefs.values.edit().putBoolean("imageBackground",true).putInt("clockOpacity",100).apply();}
        render();
    }catch(Exception error){toast("无法导入，请选择本地图片或有效字体（不超过10MB）");}finally{if(temp!=null&&temp.exists())temp.delete();}}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);String target=intent.getStringExtra("page");if(target!=null){page=target;back.clear();if(!page.equals("home"))back.push("home");render();}if(intent.getBooleanExtra("picker",false))countdownPicker();}
    @Override protected void onResume(){super.onResume();if(pendingFloating){pendingFloating=false;if(Settings.canDrawOverlays(this))requestFloating();}TimerAlarm.schedule(this,TimerStore.read(this));handler.removeCallbacks(tick);handler.post(tick);}
    @Override protected void onPause(){handler.removeCallbacks(tick);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle state){state.putString("page",page);state.putBoolean("pending",pendingFloating);state.putStringArray("back",back.toArray(new String[0]));super.onSaveInstanceState(state);}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(alerts!=null)alerts.close();if(activeSheet!=null)activeSheet.dismiss();super.onDestroy();}
    private void toast(String text){android.widget.Toast.makeText(this,text,android.widget.Toast.LENGTH_LONG).show();}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
