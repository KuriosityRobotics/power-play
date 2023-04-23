package com.kuriosityrobotics.powerplay.fieldnav;

import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.navigation.FieldNavConstants;
import com.kuriosityrobotics.powerplay.navigation.FieldNavigator;
import com.kuriosityrobotics.powerplay.navigation.Junction;
import com.kuriosityrobotics.powerplay.navigation.JunctionTypes;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.junit.jupiter.api.Test;


class FieldNavigatorTest {
    @Test
    void nearestJunctionTest(){
        Junction zerozero = FieldNavigator.getNearestJunction(Pose.zero());
        assert(zerozero.type == JunctionTypes.GROUND);
        assert(zerozero.position.equals(
                new Vector2D(1 * FieldNavConstants.GRID_SIZE, 1 * FieldNavConstants.GRID_SIZE)
        ));
    }

    @Test
    void nearestJunctionOfTypeTest(){
        Junction groundJunction = FieldNavigator.getNearestJunctionOfType(Pose.zero(), JunctionTypes.GROUND);
        assert(groundJunction.position.equals(new Vector2D(1 * FieldNavConstants.GRID_SIZE, 1 * FieldNavConstants.GRID_SIZE)));

        Junction medJunction = FieldNavigator.getNearestJunctionOfType(Pose.zero(), JunctionTypes.MEDIUM);
        assert(medJunction.position.equals(new Vector2D(2 * FieldNavConstants.GRID_SIZE, 2 * FieldNavConstants.GRID_SIZE)));
        assert(medJunction.type == JunctionTypes.MEDIUM);

        // field is symmetric so there are 2 options for this one: (2, 3) and (3, 2)
        Junction highJunction = FieldNavigator.getNearestJunctionOfType(Pose.zero(), JunctionTypes.HIGH);
        assert(highJunction.position.equals(new Vector2D(2 * FieldNavConstants.GRID_SIZE, 3 * FieldNavConstants.GRID_SIZE)));
    }

    // this is not a real test, ignore it (use if you want to test I guess)
    @Test
    void pathTest(){
        Junction j = new Junction(JunctionTypes.LOW, Pose.zero());
        Pose[] path = FieldNavigator.plotPathToJunction(
                new Pose(0, 134, 0),
                j
        );

        for(int i = 0; i < path.length; i++){
            System.out.println(path[i].x() + " " + path[i].y() + " " + path[i].orientation());
        }
    }
}
