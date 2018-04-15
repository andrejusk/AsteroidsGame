package uk.ac.reading.student.akostarevas.asteroids;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Handles messages from GameThread to GameView.
 */
class GameHandler extends Handler {

    /* Pointers to the views */
    private TextView scoreView;
    private TextView livesView;
    private TextView statusView;

    private Button difficulty;
    private Button high;
    private Button help;

    private EditText name;

    GameHandler(TextView scoreView, TextView livesView, TextView statusView,
                Button difficulty, Button high, Button help, EditText name) {
        this.scoreView = scoreView;
        this.statusView = statusView;
        this.livesView = livesView;

        this.difficulty = difficulty;
        this.high = high;
        this.help = help;

        this.name = name;
    }

    @Override
    public void handleMessage(Message m) {
        if (m.getData().getBoolean("score")) {
            this.scoreView.setVisibility(View.VISIBLE);
            scoreView.setText(m.getData().getString("text"));
        }
        else if (m.getData().getBoolean("lives")) {
            this.livesView.setVisibility(View.VISIBLE);
            livesView.setText(m.getData().getString("text"));
        }
        else if (m.getData().getBoolean("submit")) {
            /* Show input */
            name.setVisibility(View.VISIBLE);
            name.requestFocus();

            /* Open keyboard */
            InputMethodManager keyboard = (InputMethodManager)
                name.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (keyboard != null) {
                keyboard.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
            }

            /* Listen for complete */
            name.setOnEditorActionListener(new EditText.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        System.out.println(v.getText());

                        /* Close keyboard */
                        InputMethodManager keyboard = (InputMethodManager)
                                name.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                        if (keyboard != null) {
                            keyboard.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        }

                        /* Hide input */
                        name.clearFocus();
                        name.setVisibility(View.GONE);
                        return true;
                    }
                    return false;
                }
            });
        }
        else {
            int view = m.getData().getInt("viz");
            int buttons = m.getData().getInt("buttons");

            this.statusView.setVisibility(view);
            this.difficulty.setVisibility(buttons);
            this.high.setVisibility(buttons);
            this.help.setVisibility(buttons);

            if (buttons == View.VISIBLE) {
                this.scoreView.setVisibility(View.GONE);
                this.livesView.setVisibility(View.GONE);
            }

            statusView.setText(m.getData().getString("text"));
        }
    }
}