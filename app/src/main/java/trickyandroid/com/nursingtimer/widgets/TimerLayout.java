package trickyandroid.com.nursingtimer.widgets;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import butterknife.ButterKnife;
import butterknife.InjectView;
import trickyandroid.com.nursingtimer.R;
import trickyandroid.com.nursingtimer.Utils;
import trickyandroid.com.nursingtimer.widgets.timepicker.RadialPickerLayout;
import trickyandroid.com.nursingtimer.widgets.timepicker.TimePickerDialog;

/**
 * Created by paveld on 9/9/14.
 */
public abstract class TimerLayout extends RelativeLayout implements View.OnClickListener {

    @InjectView(R.id.timerIcon)
    ImageView timerIcon;
    @InjectView(R.id.timer)
    TimerTextView timerText;
    @InjectView(R.id.timerControlPanel)
    View controlPanel;
    @InjectView(R.id.timerDetailsSection)
    View timerDetailsSection;
    @InjectView(R.id.timerStart)
    ImageView timerStartBtn;
    @InjectView(R.id.timerStop)
    ImageView timerStopBtn;
    @InjectView(R.id.alarmIcon)
    ImageView alarmBtn;

    FragmentManager fragmentManager;

    private static final float INACTIVE_ALPHA = .5f;

    boolean isControlPanelVisible = false;

    public TimerLayout(Context context) {
        this(context, null);
    }

    public TimerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TimerLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    void init() {
        inflate(getContext(), R.layout.timer_layout, this);
        ButterKnife.inject(this);
        timerIcon.setImageResource(getIconResId());
        timerText.setOnClickListener(this);
        timerText.setTimerStartTimeMs(-1);
        timerStopBtn.setOnClickListener(this);
        timerStartBtn.setOnClickListener(this);
        timerDetailsSection.setOnClickListener(this);
        alarmBtn.setOnClickListener(this);
        applyAccents();

        timerText.setAlpha(INACTIVE_ALPHA);
        alarmBtn.setAlpha(INACTIVE_ALPHA);
    }

    private void applyAccents() {
        ColorFilter cf = new PorterDuffColorFilter(getResources().getColor(R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
        timerIcon.setColorFilter(cf);
    }

    abstract int getIconResId();

    @Override
    public void onClick(View view) {
        if (getTimerStatus() == TimerTextView.TimerStatus.STOPPED && !isControlPanelVisible) {
            showControlPanel();
            return;
        }
        switch (view.getId()) {
            case R.id.timerStop:
                stopTimer();
                hideControlPanel();
                break;
            case R.id.timerStart:
                resetTimer();
                hideControlPanel();
                Utils.getPulseAnimator(timerText, .95f, 1.05f).start();
                break;
            case R.id.timer:
                showControlPanel();
                break;
            case R.id.alarmIcon:
                showNumberPicker();
                break;
        }
    }

    public void setFragmentManager(FragmentManager fm) {
        fragmentManager = fm;
    }

    private void showNumberPicker() {
        TimePickerDialog tpd = TimePickerDialog.newInstance(new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
            }

            @Override
            public void onDelete() {

            }
        }, 1, 1, true);
        tpd.show(fragmentManager, "");
    }


    private void fadeInViews(View... views) {
        AnimatorSet set = new AnimatorSet();
        Animator[] animators = new Animator[views.length];
        for (int i = 0; i < views.length; i++) {
            animators[i] = ObjectAnimator.ofFloat(views[i], View.ALPHA, 1);
        }
        set.playTogether(animators);
        set.start();
    }

    private void fadeOutViews(View... views) {
        AnimatorSet set = new AnimatorSet();
        Animator[] animators = new Animator[views.length];
        for (int i = 0; i < views.length; i++) {
            animators[i] = ObjectAnimator.ofFloat(views[i], View.ALPHA, INACTIVE_ALPHA);
        }
        set.playTogether(animators);
        set.start();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putInt("timerStatus", this.timerText.getCurrentTimerStatus().ordinal());
        bundle.putLong("timerTime", this.timerText.getTimerStartTimeMs());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            state = bundle.getParcelable("instanceState");
            timerText.setTimerStartTimeMs(bundle.getLong("timerTime", 0));
            TimerTextView.TimerStatus status = TimerTextView.TimerStatus.values()[bundle.getInt("timerStatus")];
            if (status == TimerTextView.TimerStatus.STARTED) {
                resumeTimer();
            }
        }
        super.onRestoreInstanceState(state);
    }

    public TimerTextView.TimerStatus getTimerStatus() {
        return this.timerText.getCurrentTimerStatus();
    }

    public void pauseTimer() {
        this.timerText.pauseTimer();
    }

    public void stopTimer() {
        this.timerText.stopTimer();
        fadeOutViews(this.timerText, this.alarmBtn);
    }

    public void resumeTimer() {
        this.timerText.startTimer();
        fadeInViews(this.timerText, this.alarmBtn);
    }

    public void resetTimer() {
        this.timerText.resetTimer();
        fadeInViews(this.timerText, this.alarmBtn);
    }

    public void showControlPanel() {
        if (isControlPanelVisible) {
            return;
        }


        if (timerText.getCurrentTimerStatus() == TimerTextView.TimerStatus.STOPPED) {
            timerStartBtn.setImageResource(R.drawable.ic_action_play);
            timerStopBtn.setVisibility(View.GONE);
        } else {
            timerStartBtn.setImageResource(R.drawable.ic_action_replay);
            timerStopBtn.setVisibility(View.VISIBLE);
        }

        isControlPanelVisible = true;
        controlPanel.setOnFocusChangeListener(controlPanelFocusChangeListener);
        controlPanel.setVisibility(View.VISIBLE);
        controlPanel.requestFocus();
        controlPanel.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                controlPanel.getViewTreeObserver().removeOnPreDrawListener(this);
                controlPanel.setTranslationX(controlPanel.getWidth());
                controlPanel.animate().translationX(0).setInterpolator(new DecelerateInterpolator(2)).start();
                return true;
            }
        });
    }

    public void hideControlPanel() {
        if (!isControlPanelVisible) {
            return;
        }
        isControlPanelVisible = false;
        controlPanel.setOnFocusChangeListener(null);
        controlPanel.animate().translationX(controlPanel.getWidth()).setInterpolator(new DecelerateInterpolator(2)).withEndAction(new Runnable() {
            @Override
            public void run() {
                controlPanel.setVisibility(View.GONE);
            }
        });
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isControlPanelVisible) {
            if (ev.getX() < controlPanel.getLeft()) {
                hideControlPanel();
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    OnFocusChangeListener controlPanelFocusChangeListener = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean b) {
            if (!b) {
                hideControlPanel();
            }
        }
    };
}