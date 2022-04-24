package bdt.source;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import bdt.source.SourceCoordinate;


public class SourceCoordinateTests {

  @Test
  public void testEmpty() {
    long coord = SourceCoordinate.createEmpty();
    assertEquals(0, SourceCoordinate.getStartIndex(coord));
    assertEquals(0, SourceCoordinate.getLength(coord));
  }

  @Test
  public void testSmallOnes() {
    long coord = SourceCoordinate.create(0, 1);
    assertEquals(0, SourceCoordinate.getStartIndex(coord));
    assertEquals(1, SourceCoordinate.getLength(coord));

    coord = SourceCoordinate.create(0, 10);
    assertEquals(0, SourceCoordinate.getStartIndex(coord));
    assertEquals(10, SourceCoordinate.getLength(coord));

    coord = SourceCoordinate.create(32, 45);
    assertEquals(32, SourceCoordinate.getStartIndex(coord));
    assertEquals(45, SourceCoordinate.getLength(coord));
  }

  @Test
  public void testRegression() {
    long coord = SourceCoordinate.create(905, 4);
    assertEquals(905, SourceCoordinate.getStartIndex(coord));
    assertEquals(4, SourceCoordinate.getLength(coord));
  }

  @Test
  public void testBigOnes() {
    long coord = SourceCoordinate.create(0xFFFFFF, 0xEEEEEE);
    assertEquals(0xFFFFFF, SourceCoordinate.getStartIndex(coord));
    assertEquals(0xEEEEEE, SourceCoordinate.getLength(coord));

    coord = SourceCoordinate.create(0, 0xAAAAAAAA);
    assertEquals(0, SourceCoordinate.getStartIndex(coord));
    assertEquals(0xAAAAAAAA, SourceCoordinate.getLength(coord));

    coord = SourceCoordinate.create(0xFFFFFFFF, 0);
    assertEquals(0xFFFFFFFF, SourceCoordinate.getStartIndex(coord));
    assertEquals(0, SourceCoordinate.getLength(coord));

    coord = SourceCoordinate.create(0xFFFFFFFF, 0xAAAAAAAA);
    assertEquals(0xFFFFFFFF, SourceCoordinate.getStartIndex(coord));
    assertEquals(0xAAAAAAAA, SourceCoordinate.getLength(coord));

    coord = SourceCoordinate.create(0xFFFFFFFF, 0xEEEEEEEE);
    assertEquals(0xFFFFFFFF, SourceCoordinate.getStartIndex(coord));
    assertEquals(0xEEEEEEEE, SourceCoordinate.getLength(coord));

    coord = SourceCoordinate.create(0xFFFFFFFF, 0xFFFFFFFF);
    assertEquals(0xFFFFFFFF, SourceCoordinate.getStartIndex(coord));
    assertEquals(0xFFFFFFFF, SourceCoordinate.getLength(coord));
  }
}
