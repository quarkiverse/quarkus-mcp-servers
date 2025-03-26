///usr/bin/env jbang "$0" "$@" ; exit $?
package io.quarkiverse.mcp.servers.jfx;

//JAVA 17+
//DEPS org.openjfx:javafx-controls:21:${os.detected.jfxname}
//DEPS org.openjfx:javafx-graphics:21:${os.detected.jfxname}
//DEPS org.openjfx:javafx-swing:21:${os.detected.jfxname}
//DEPS io.quarkiverse.fx:quarkus-fx:0.9.1
//DEPS io.quarkus:quarkus-bom:3.19.2@pom
//DEPS io.quarkiverse.mcp:quarkus-mcp-server-stdio:1.0.0.Beta5
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkiverse.mcp.server.ImageContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkiverse.mcp.server.WrapBusinessError;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.stage.Stage;

@ApplicationScoped
@WrapBusinessError(java.lang.IllegalStateException.class)
public class MCPServerJFX {
    Stage stage;
    private static Canvas canvas;
    private static GraphicsContext gc;
    private static double canvasWidth = 800;
    private static double canvasHeight = 600;

    public void start(@Observes final FxPostStartupEvent event) {
        stage = event.getPrimaryStage();

    }

    void runAndWait(Runnable runnable) {
        final CountDownLatch latchToWaitForJavaFx = new CountDownLatch(1);

        Platform.runLater(() -> {
            runnable.run();
            latchToWaitForJavaFx.countDown();
        });

        try {
            latchToWaitForJavaFx.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new ToolCallException("Failed to wait for JavaFX to finish");
        }
    }

    @Tool(description = "Launch a new drawing canvas")
    String launchCanvas() {

        Platform.runLater(() -> {
            if (stage.getScene() != null) {
                stage.hide();
            }

            canvas = new Canvas(canvasWidth, canvasHeight);
            gc = canvas.getGraphicsContext2D();

            // Set default properties
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);

            Scene scene = new Scene(new StackPane(canvas), canvasWidth, canvasHeight);
            stage.setScene(scene);
            stage.show();
        });

        return "Canvas launched with dimensions: " + canvasWidth + "x" + canvasHeight;
    }

    @Tool(description = "Close the drawing canvas")
    String closeCanvas() {
        Platform.runLater(() -> {
            stage.hide();
        });
        return "Canvas closed";
    }

    @Tool(description = "Draw a line on the canvas")
    String drawLine(
            @ToolArg(description = "Starting X coordinate") double startX,
            @ToolArg(description = "Starting Y coordinate") double startY,
            @ToolArg(description = "Ending X coordinate") double endX,
            @ToolArg(description = "Ending Y coordinate") double endY) {
        if (gc == null)
            return "Canvas not initialized. Launch canvas first.";

        Platform.runLater(() -> {
            gc.strokeLine(startX, startY, endX, endY);
        });
        return "Line drawn from (" + startX + "," + startY + ") to (" + endX + "," + endY + ")";
    }

    @Tool(description = "Draw a rectangle on the canvas")
    String drawRectangle(
            @ToolArg(description = "X coordinate") double x,
            @ToolArg(description = "Y coordinate") double y,
            @ToolArg(description = "Width") double width,
            @ToolArg(description = "Height") double height,
            @ToolArg(description = "Fill the rectangle (true/false)") boolean fill) {
        if (gc == null)
            return "Canvas not initialized. Launch canvas first.";

        Platform.runLater(() -> {
            if (fill) {
                gc.fillRect(x, y, width, height);
            } else {
                gc.strokeRect(x, y, width, height);
            }
        });
        return "Rectangle drawn at (" + x + "," + y + ") with dimensions: " + width + "x" + height;
    }

    @Tool(description = "Draw a circle on the canvas")
    String drawCircle(
            @ToolArg(description = "Center X coordinate") double centerX,
            @ToolArg(description = "Center Y coordinate") double centerY,
            @ToolArg(description = "Radius") double radius,
            @ToolArg(description = "Fill the circle (true/false)") boolean fill) {
        if (gc == null)
            return "Canvas not initialized. Launch canvas first.";

        Platform.runLater(() -> {
            if (fill) {
                gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            } else {
                gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            }
        });
        return "Circle drawn at (" + centerX + "," + centerY + ") with radius: " + radius;
    }

    @Tool(description = "Draw an arc on the canvas")
    String drawArc(
            @ToolArg(description = "Center X coordinate") double x,
            @ToolArg(description = "Center Y coordinate") double y,
            @ToolArg(description = "Radius X") double radiusX,
            @ToolArg(description = "Radius Y") double radiusY,
            @ToolArg(description = "Start angle in degrees") double startAngle,
            @ToolArg(description = "Arc extent in degrees") double arcExtent,
            @ToolArg(description = "Fill the arc (true/false)") boolean fill) {
        if (gc == null)
            return "Canvas not initialized. Launch canvas first.";

        Platform.runLater(() -> {
            if (fill) {
                gc.fillArc(x - radiusX, y - radiusY, radiusX * 2, radiusY * 2,
                        startAngle, arcExtent, ArcType.ROUND);
            } else {
                gc.strokeArc(x - radiusX, y - radiusY, radiusX * 2, radiusY * 2,
                        startAngle, arcExtent, ArcType.ROUND);
            }
        });

        return String.format("Arc drawn at (%.1f,%.1f) with radii: %.1fx%.1f",
                x, y, radiusX, radiusY);
    }

    @Tool(description = "Draw a cubic bezier curve")
    String drawBezier(
            @ToolArg(description = "Start X") double startX,
            @ToolArg(description = "Start Y") double startY,
            @ToolArg(description = "Control point 1 X") double controlX1,
            @ToolArg(description = "Control point 1 Y") double controlY1,
            @ToolArg(description = "Control point 2 X") double controlX2,
            @ToolArg(description = "Control point 2 Y") double controlY2,
            @ToolArg(description = "End X") double endX,
            @ToolArg(description = "End Y") double endY,
            @ToolArg(description = "Stroke color (e.g., BLACK, #FF0000)") String strokeColor,
            @ToolArg(description = "Fill color (optional, e.g., BLUE, #00FF00)") String fillColor,
            @ToolArg(description = "Line width") double lineWidth,
            @ToolArg(description = "Close path (true/false)") boolean closePath) {
        if (gc == null)
            return "Canvas not initialized. Launch canvas first.";

        Platform.runLater(() -> {
            gc.save(); // Save the current state

            // Set stroke color and line width
            gc.setStroke(Color.web(strokeColor));
            gc.setLineWidth(lineWidth);

            gc.beginPath();
            gc.moveTo(startX, startY);
            gc.bezierCurveTo(controlX1, controlY1, controlX2, controlY2, endX, endY);

            if (closePath) {
                gc.closePath();
            }

            // Fill if a fill color is provided
            if (fillColor != null && !fillColor.isEmpty()) {
                gc.setFill(Color.web(fillColor));
                gc.fill();
            }

            gc.stroke();

            gc.restore(); // Restore the original state
        });

        return String.format("Bezier curve drawn from (%.1f,%.1f) to (%.1f,%.1f)",
                startX, startY, endX, endY);
    }

    @Tool(description = "Create a linear gradient fill")
    String setLinearGradient(
            @ToolArg(description = "Start X") double startX,
            @ToolArg(description = "Start Y") double startY,
            @ToolArg(description = "End X") double endX,
            @ToolArg(description = "End Y") double endY,
            @ToolArg(description = "List of color stops (format: [0.0,#color1,0.5,#color2,...]") String[] stops) {
        if (gc == null)
            return "Canvas not initialized. Launch canvas first.";

        Platform.runLater(() -> {
            List<Stop> gradientStops = new ArrayList<>();
            for (int i = 0; i < stops.length; i += 2) {
                double offset = Double.parseDouble(stops[i]);
                Color color = Color.web(stops[i + 1]);
                gradientStops.add(new Stop(offset, color));
            }

            LinearGradient gradient = new LinearGradient(
                    startX, startY, endX, endY, false, CycleMethod.NO_CYCLE,
                    gradientStops);
            gc.setFill(gradient);
        });

        return "Linear gradient created and set as fill";
    }

    @Tool(description = "Draw a polygon")
    String drawPolygon(
            @ToolArg(description = "Array of X coordinates") double[] xPoints,
            @ToolArg(description = "Array of Y coordinates") double[] yPoints,
            @ToolArg(description = "Fill the polygon (true/false)") boolean fill) {
        if (gc == null)
            return "Canvas not initialized. Launch canvas first.";
        if (xPoints.length != yPoints.length)
            return "Error: X and Y arrays must have the same length";

        Platform.runLater(() -> {
            gc.beginPath();
            gc.moveTo(xPoints[0], yPoints[0]);
            for (int i = 1; i < xPoints.length; i++) {
                gc.lineTo(xPoints[i], yPoints[i]);
            }
            gc.closePath();

            if (fill) {
                gc.fill();
            } else {
                gc.stroke();
            }
        });

        return String.format("Polygon drawn with %d points", xPoints.length);
    }

    @Tool(description = "Draw text on the canvas")
    String drawText(
            @ToolArg(description = "Text to draw") String text,
            @ToolArg(description = "X coordinate") double x,
            @ToolArg(description = "Y coordinate") double y,
            @ToolArg(description = "Font size") double fontSize,
            @ToolArg(description = "Font family") String fontFamily) {
        if (gc == null)
            return "Canvas not initialized. Launch canvas first.";

        Platform.runLater(() -> {
            Font font = new Font(fontFamily, fontSize);
            gc.setFont(font);
            gc.fillText(text, x, y);
        });

        return String.format("Text '%s' drawn at (%.1f,%.1f)", text, x, y);
    }

    @Tool(description = "Set the drawing color")
    String setColor(
            @ToolArg(description = "Color name (e.g., BLACK, RED, BLUE, GREEN) or hexadecimal format: #RRGGBB") String colorName) {
        if (gc == null)
            return "Canvas not initialized. Launch canvas first.";

        try {
            Color color = Color.valueOf(colorName.toUpperCase());
            Platform.runLater(() -> {
                gc.setStroke(color);
                gc.setFill(color);
            });
            return "Color set to: " + colorName;
        } catch (IllegalArgumentException e) {
            return "Invalid color name. Try using basic color names like BLACK, RED, BLUE, GREEN";
        }
    }

    @Tool(description = "Clear the canvas")
    String clearCanvas() {
        if (gc == null)
            return "Canvas not initialized. Launch canvas first.";

        Platform.runLater(() -> {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        });
        return "Canvas cleared";
    }

    @Tool(description = "Set line width for drawing")
    String setLineWidth(
            @ToolArg(description = "Line width in pixels") double width) {
        if (gc == null)
            return "Canvas not initialized. Launch canvas first.";

        Platform.runLater(() -> {
            gc.setLineWidth(width);
        });
        return "Line width set to: " + width;
    }

    @Tool(description = "Get the current canvas image as base64-encoded string")
    ImageContent getCanvasImage() {
        if (gc == null)
            return null;

        try {
            final WritableImage[] writableImage = new WritableImage[1];
            Platform.runLater(() -> {
                writableImage[0] = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                canvas.snapshot(new SnapshotParameters(), writableImage[0]);
            });

            // Wait for the JavaFX thread to complete
            while (writableImage[0] == null) {
                Thread.sleep(100);
            }

            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage[0], null);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", outputStream);

            return new ImageContent(Base64.getEncoder().encodeToString(outputStream.toByteArray()), "image/png");
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }
}
