package edu.fvtc.dragdropdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;

/*
https://snippets.cacher.io/snippet/875030698b29c73f56db
 */

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    HubConnection hubConnection;
    String hubConnectionId;
    int width;
    int height;
    Board board;
    Point point  = new Point();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);

        setContentView(new DrawView(this));

        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        */

        initSignalR();

        getScreenDims();
        board = new Board(width);

        Log.d(TAG, "onCreate: End");
    }

    private void getScreenDims() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        Log.d(TAG, "getScreenDims: " + width + ":" + height);
    }

    private void initSignalR() {
        hubConnection = HubConnectionBuilder
                .create("https://fvtcdp.azurewebsites.net/GameHub")
                .build();
        Log.d(TAG, "initSignalR: Hub Built");

        hubConnection.start().blockingAwait();
        hubConnection.invoke(Void.class, "GetConnectionId");

        hubConnection.on("ReceiveMessage", (user, message) -> {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: User: " + user);
                    Log.d(TAG, "run: Message: " + message);

                    String[] data = user.split(":", 3);
                    setContentView(new DrawView(getMainActivity(),
                            Integer.valueOf(data[1]),
                            Integer.valueOf(data[2])));

                }
            });
        }, String.class, String.class);
    }

    private MainActivity getMainActivity()
    {
        return this;
    }
    private class DrawView extends View implements View.OnTouchListener{
        int lastTouchX;
        int lastTouchY;
        int posX = 140;
        int posY = 130;

        int dx;
        int dy;
        int offsetX;
        int offsetY;
        Boolean isDragging;

        String turn = "1";


        Bitmap aceOfSpades = BitmapFactory.decodeResource(getResources(), R.drawable.acespades);
        Bitmap aceOfHearts = BitmapFactory.decodeResource(getResources(), R.drawable.acehearts);

        public DrawView(Context context) {
            super(context);
            this.setOnTouchListener(this);
        }

        public DrawView(Context context, int x, int y) {
            super(context);
            this.setOnTouchListener(this);
            posX = x;
            posY = y;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            int x = (int) event.getX();
            int y = (int) event.getY();

            Log.d(TAG, "onTouch: " + x + ":" + y);

            switch(event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "onTouch: Down");
                   /* Rect box = new Rect(posX,
                                        posY,
                                        posX + aceOfSpades.getWidth(),
                                        posY + aceOfSpades.getHeight());

                    // If not click in box, get out
                    if(!box.contains(x, y)) break;

                    offsetX = x - posX;
                    offsetY = y - posY;

                    isDragging = true;
                    lastTouchX = x;
                    lastTouchY = y;*/

                    point.x = (int)event.getX();
                    point.y = (int)event.getY();

                    if(board.hitTest(point, turn, this.getContext())!= "-1")
                    {
                        // Flip the turn
                        turn = (turn == "1") ? "2" : "1";
                    }

                    invalidate();

                    break;

                case MotionEvent.ACTION_MOVE:
                    Log.d(TAG, "onTouch: Move");

                    if(!isDragging) break;

                    dx = x - lastTouchX;
                    dy = y - lastTouchY;

                    lastTouchX = x;
                    lastTouchY = y;

                    posX += dx;
                    posY += dy;

                    invalidate();
                    break;

                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "onTouch: Up");
                    hubConnection.send("SendMessage", "Brian:" + (x-offsetX) + ":" + (y - offsetY), "Brian");
                    isDragging = false;
            }

            return true;
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            //super.onDraw(canvas);
            //canvas.drawBitmap(aceOfSpades, posX, posY, null);
            canvas.drawColor(Color.DKGRAY);
            board.Draw(canvas);

        }
    }
}