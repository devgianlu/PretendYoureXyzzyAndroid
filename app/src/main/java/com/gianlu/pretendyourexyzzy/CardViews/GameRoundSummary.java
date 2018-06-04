package com.gianlu.pretendyourexyzzy.CardViews;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.gianlu.commonutils.FontsManager;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.GameRound;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.RoundCard;

import java.util.Random;

// TODO: Multiple cards
public class GameRoundSummary {
    private final static int PADDING = 48;
    private final static int CARD_HEIGHT = 512;
    private final static int CARD_WIDTH = 348;
    private final static int MAX_COLUMNS = 5;
    private final static int CARD_RADIUS = 24;
    private final static int CARD_INTERNAL_PADDING = 36;
    private final static int MAX_TEXT_SIZE = 64;
    private final static int SHADOW_DX = -18;
    private final static int SHADOW_DY = 18;
    private final static int SHADOW_RADIUS = 4;
    private final static int SHADOW_COLOR = 0x80B2B2B2;
    private final static int BACKGROUND_COLOR = 0xFFE5E5E5;
    private final static int WINNER_COLOR = 0xFF4CC7FF;
    private static final int MAX_ROTATION = 10;
    private final static int WATERMARK_TEXT_SIZE = 48;
    private final GameRound round;
    private final boolean rotate;
    private final Bitmap bitmap;
    private final Canvas canvas;
    private final Paint blackPaint;
    private final Paint whitePaint;
    private final Paint winnerPaint;
    private final TextPaint whiteTextPaint;
    private final TextPaint blackTextPaint;
    private final Paint backgroundPaint;
    private final Random random = new Random();
    private final TextPaint grayTextPaint;
    private int rows;
    private int cols;

    public GameRoundSummary(@NonNull Context context, @NonNull GameRound round, boolean rotate) {
        this.round = round;
        this.rotate = rotate;

        blackPaint = new Paint();
        blackPaint.setColor(Color.BLACK);
        blackPaint.setAntiAlias(true);
        blackPaint.setShadowLayer(SHADOW_RADIUS, SHADOW_DX, SHADOW_DY, SHADOW_COLOR);

        blackTextPaint = new TextPaint();
        blackTextPaint.setAntiAlias(true);
        blackTextPaint.setColor(Color.BLACK);
        blackTextPaint.setTypeface(FontsManager.get().get(context, FontsManager.ROBOTO_MEDIUM));

        whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setAntiAlias(true);
        whitePaint.setShadowLayer(SHADOW_RADIUS, SHADOW_DX, SHADOW_DY, SHADOW_COLOR);

        whiteTextPaint = new TextPaint();
        whiteTextPaint.setColor(Color.WHITE);
        whiteTextPaint.setAntiAlias(true);
        whiteTextPaint.setTypeface(FontsManager.get().get(context, FontsManager.ROBOTO_MEDIUM));

        winnerPaint = new Paint();
        winnerPaint.setColor(WINNER_COLOR);
        winnerPaint.setAntiAlias(true);
        winnerPaint.setShadowLayer(SHADOW_RADIUS, SHADOW_DX, SHADOW_DY, SHADOW_COLOR);

        grayTextPaint = new TextPaint();
        grayTextPaint.setColor(Color.DKGRAY);
        grayTextPaint.setAntiAlias(true);
        grayTextPaint.setTypeface(FontsManager.get().get(context, FontsManager.ROBOTO_MEDIUM));

        backgroundPaint = new Paint();
        backgroundPaint.setColor(BACKGROUND_COLOR);

        bitmap = Bitmap.createBitmap(measureWidth(), measureHeight(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        draw();
    }

    private int measureHeight() {
        rows = (int) Math.ceil(round.whiteCards() / (float) MAX_COLUMNS);
        return PADDING * (rows + 1) + CARD_HEIGHT * rows;
    }

    private int measureWidth() {
        int whites = round.whiteCards();
        cols = Math.min(MAX_COLUMNS, whites);
        cols++; // Black card
        return PADDING * (cols + 1) + CARD_WIDTH * cols;
    }

    private void drawCard(int x, int y, boolean winner, RoundCard card) {
        Paint paint;
        TextPaint textPaint;
        TextPaint watermarkPaint;
        if (winner) {
            paint = winnerPaint;
            textPaint = whiteTextPaint;
            watermarkPaint = whiteTextPaint;
        } else {
            paint = card.black() ? blackPaint : whitePaint;
            textPaint = card.black() ? whiteTextPaint : blackTextPaint;
            watermarkPaint = card.black() ? whiteTextPaint : grayTextPaint;
        }

        RectF rect = new RectF(x, y, x + CARD_WIDTH, y + CARD_HEIGHT);
        int maxTextHeight = (int) (rect.height() - CARD_INTERNAL_PADDING * 2);
        int maxTextWidth = (int) (rect.width() - CARD_INTERNAL_PADDING * 2);

        canvas.save();
        if (rotate)
            canvas.rotate(-MAX_ROTATION + random.nextFloat() * MAX_ROTATION * 2, rect.centerX(), rect.centerY());

        canvas.drawRoundRect(rect, CARD_RADIUS, CARD_RADIUS, paint);

        StaticLayout text;

        canvas.save();
        watermarkPaint.setTextSize(WATERMARK_TEXT_SIZE);
        text = new StaticLayout(card.watermark, watermarkPaint, maxTextWidth, Layout.Alignment.ALIGN_OPPOSITE, 1, 0, true);
        maxTextHeight -= text.getHeight();
        canvas.translate(rect.left, rect.top + maxTextHeight + CARD_INTERNAL_PADDING);
        text.draw(canvas);
        canvas.restore();

        textPaint.setTextSize(MAX_TEXT_SIZE);
        do {
            text = new StaticLayout(card.text(), textPaint, maxTextWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, true);
            textPaint.setTextSize(textPaint.getTextSize() - 1);
        } while (text.getHeight() >= maxTextHeight);

        canvas.translate(rect.left + CARD_INTERNAL_PADDING, rect.top + CARD_INTERNAL_PADDING);
        text.draw(canvas);
        canvas.restore();
    }

    private void draw() {
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);

        drawCard(PADDING, PADDING, false, round.blackCard);
        drawCard(PADDING * 2 + CARD_WIDTH, PADDING, true, round.winningCard.get(0));

        int i = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = row == 0 ? 2 : 1; col < cols; col++) {
                if (i < round.otherCards.size()) {
                    RoundCard card = round.otherCards.get(i).get(0);
                    drawCard(PADDING * (col + 1) + CARD_WIDTH * col, PADDING * (row + 1) + CARD_HEIGHT * row, false, card);
                    i++;
                } else {
                    break;
                }
            }
        }
    }

    @NonNull
    public Bitmap getBitmap() {
        return bitmap;
    }

    @NonNull
    public String getName() {
        return "PYX GameRound - " + round.gameId + " - " + round.timestamp;
    }
}
