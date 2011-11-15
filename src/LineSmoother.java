import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * A method to smooth a hand-drawn line based on the McMaster 
 * line smoothing algorithm
 * 
 * @author Derek Springer
 */
public class LineSmoother {

    /**
     * @param lineSegments A list of line segments representing a line
     * @return A list line segments representing the smoothed line
     */
    public static List<CustomLine> smoothLine(List<CustomLine> lineSegments) {
        if(lineSegments.size() < 4) return lineSegments; 

        List<CustomLine> smoothedLine = new ArrayList<CustomLine>();
        List<Point> points = getPoints(lineSegments);
        smoothedLine.add(lineSegments.get(0));

        Point newPoint = points.get(1);
        for(int i = 2; i < points.size()-2; i++) {
            Point lastPoint = newPoint;
            newPoint = smoothPoint(points.subList(i-2, i+3));
            smoothedLine.add(new CustomLine(lastPoint, newPoint));
        }

        CustomLine lastSegment = lineSegments.get(lineSegments.size()-1);
        smoothedLine.add(new CustomLine(
                newPoint, 
                new Point(lastSegment.x1, lastSegment.y1)));
        smoothedLine.add(lastSegment);

        return smoothedLine;
    }

    /**
     * @param lineSegments A list of line segments representing a line
     * @return A list of Points representing the points in a series of 
     *  line segments
     */
    public static List<Point> getPoints(List<CustomLine> lineSegments) {
        List<Point> points = new ArrayList<Point>();
        for(CustomLine segment : lineSegments) {
            points.add(new Point(segment.x1, segment.y1));
        }
        points.add(new Point(
                lineSegments.get(lineSegments.size()-1).x2, 
                lineSegments.get(lineSegments.size()-1).y2));

        return points;
    }

    /**
     * Find the new point for a smoothed line segment 
     * @param points The five points needed
     * @return The new point for the smoothed line segment
     */
    public static Point smoothPoint(List<Point> points) {
        int avgX = 0;
        int avgY = 0;
        for(Point point : points) {
            avgX += point.x;
            avgY += point.y;
        }

        avgX = avgX/points.size();
        avgY = avgY/points.size();
        Point newPoint = new Point(avgX, avgY);
        Point oldPoint = points.get(points.size()/2);
        int newX = (newPoint.x + oldPoint.x)/2;
        int newY = (newPoint.y + oldPoint.y)/2;

        return new Point(newX, newY);
    }
}