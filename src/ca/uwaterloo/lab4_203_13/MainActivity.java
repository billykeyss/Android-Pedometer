package ca.uwaterloo.lab4_203_13;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

	// Mapping variables
	List<PointF> path = new ArrayList<PointF>();
	Location currentPosition = new Location();
	PointF userPosition;
	float theta;
	PointF nextStep;
	TextView arrived;
	String status;
	float calibrate;
	int firstTry = 0;

	// Instruction variables
	String eastWest;
	String northSouth;
	float stepsX;
	float stepsY;
	TextView instructions;

	// Used for formating
	DecimalFormat df = new DecimalFormat("#.##");

	// Declares Map
	MapView mapView;

	// Declares textview variables
	TextView stepDisplay;
	TextView accelMax;
	TextView accelMin;
	TextView as2;
	TextView position;
	TextView mOrientationData;

	// Declares Graph
	LineGraphView graph;

	// Declares Button
	Button clearRecord;
	Button stepIncrease;
	Button NorthIncrease;
	Button SouthIncrease;
	Button WestIncrease;
	Button EastIncrease;
	Button calibration;

	// Declares SensorManager
	private SensorManager mSensMan;

	// Decalres variables necessary for pedometer
	public float[] accelRecordArray = new float[] { 0, 0, 0 };
	public float[] accelRecordLowArray = new float[] { 0, 0, 0 };
	public float[] positionArray = new float[] { 0, 0 };
	public float[] positionFromStartArray = new float[] { 0, 0 };
	private float[] gravMatrix = new float[3];
	private float[] geoMagMatrix = new float[3];
	private float[] mOrientation = new float[3];
	private float[] mRotation = new float[9];
	float previousAngle;
	float currentAngle;
	int stepCounter = 0;
	float displacement;

	// Delcares the maps
	NavigationalMap map1;

	/*
	 * NavigationalMap map2; NavigationalMap map3; NavigationalMap map4;
	 * NavigationalMap map5; NavigationalMap map6; NavigationalMap map7;
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		mapView.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return super.onContextItemSelected(item)
				|| mapView.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Setting Layout
		LinearLayout top = (LinearLayout) findViewById(R.id.label2);
		top.setOrientation(LinearLayout.VERTICAL);

		arrived = new TextView(getApplicationContext());
		arrived.setText("Set Origin and Destination");
		top.addView(arrived);

		instructions = new TextView(getApplicationContext());
		top.addView(instructions);

		// Adding Maps
		mapView = new MapView(getApplicationContext(), 700, 500, 30, 30);
		mapView.addListener(currentPosition);
		registerForContextMenu(mapView);

		// Loads the maps
		map1 = MapLoader.loadMap(getExternalFilesDir(null),
				"Lab-room-peninsula.svg");
		/*
		 * map2 = MapLoader.loadMap(getExternalFilesDir(null),
		 * "Lab-room-peninsula-16deg.svg"); map3 =
		 * MapLoader.loadMap(getExternalFilesDir(null),
		 * "Lab-room-peninsula-9.4deg.svg"); map4 =
		 * MapLoader.loadMap(getExternalFilesDir(null),
		 * "Lab-room-inclined-9.4deg.svg"); map5 =
		 * MapLoader.loadMap(getExternalFilesDir(null),
		 * "Lab-room-inclined-16deg.svg"); map6 =
		 * MapLoader.loadMap(getExternalFilesDir(null),
		 * "Lab-room-unconnected.svg"); map7 =
		 * MapLoader.loadMap(getExternalFilesDir(null), "Lab-room.svg");
		 */
		mapView.setMap(map1);
		top.addView(mapView);
		userPosition = mapView.getUserPoint();

		// Adding Graph
		graph = new LineGraphView(getApplicationContext(), 100, Arrays.asList(
				"x", "y", "z"));
		graph.setVisibility(View.VISIBLE);
		top.addView(graph);

		// Pedometer Variables
		as2 = new TextView(getApplicationContext());
		stepDisplay = new TextView(getApplicationContext());
		accelMax = new TextView(getApplicationContext());
		accelMin = new TextView(getApplicationContext());
		mOrientationData = new TextView(getApplicationContext());
		position = new TextView(getApplicationContext());
		top.addView(mOrientationData);
		top.addView(stepDisplay);
		top.addView(position);

		// register Sensor
		mSensMan = (SensorManager) getSystemService(SENSOR_SERVICE);
		SensorEventListener eventListener = new AccelerationSensorEventListener(
				as2);

		// Magnetic Sensor
		Sensor magSensor = mSensMan
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensMan.registerListener(eventListener, magSensor,
				SensorManager.SENSOR_DELAY_UI);

		// Acceleration Sensor
		Sensor acceleration = mSensMan
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensMan.registerListener(eventListener, acceleration,
				SensorManager.SENSOR_DELAY_UI);

		// Linear Acceleration Sensor
		SensorManager sm2 = (SensorManager) getSystemService(SENSOR_SERVICE);
		Sensor acceleration2 = sm2
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		SensorEventListener l2 = new AccelerationSensorEventListener(as2);
		sm2.registerListener(l2, acceleration2, SensorManager.SENSOR_DELAY_UI);

		// Defining Buttons
		clearRecord = (Button) findViewById(R.id.button);
		clearRecord.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				// Resets the step counter and displacement meter
				Arrays.fill(accelRecordArray, 0);
				Arrays.fill(positionFromStartArray, 0);
				stepCounter = 0;

				// Resets graph
				graph.purge();

				// Resets calibration
				calibrate = 0;

				// Resets origin, destination, and user point
				mapView.setOriginPoint(0, 0);
				mapView.setDestinationPoint(0, 0);
				mapView.setUserPoint(0, 0);
			}
		});

		calibration = (Button) findViewById(R.id.calibratebutton);
		calibration.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				calibrateNorth();
			}
		});

		stepIncrease = (Button) findViewById(R.id.StepIncrease);
		stepIncrease.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stepCounter++;
				positionFromStartArray[0] += positionArray[0];
				positionFromStartArray[1] += positionArray[1];
				updateUserPosition(userPosition, theta);

				statusUpdate();
			}
		});

		NorthIncrease = (Button) findViewById(R.id.NorthIncrease);
		NorthIncrease.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stepCounter++;
				positionFromStartArray[0] += 0;
				positionFromStartArray[1] += 1;
				updateUserPosition(userPosition, 0);

				statusUpdate();
			}
		});

		EastIncrease = (Button) findViewById(R.id.EastIncrease);
		EastIncrease.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stepCounter++;
				positionFromStartArray[0] += 1;
				positionFromStartArray[1] += 0;
				updateUserPosition(userPosition, (float) 1.570796327);

				statusUpdate();
			}
		});

		SouthIncrease = (Button) findViewById(R.id.SouthIncrease);
		SouthIncrease.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stepCounter++;
				positionFromStartArray[0] += 0;
				positionFromStartArray[1] += -1;
				updateUserPosition(userPosition, (float) 3.141592654);

				statusUpdate();
			}
		});

		WestIncrease = (Button) findViewById(R.id.WestIncrease);
		WestIncrease.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stepCounter++;
				positionFromStartArray[0] += -1;
				positionFromStartArray[1] += 0;
				updateUserPosition(userPosition, (float) -1.570796327);

				statusUpdate();
			}
		});

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// Pedometer Code
	class AccelerationSensorEventListener implements SensorEventListener {

		TextView output;
		int firstTime;
		int state;
		long lastStep = 0;
		boolean ready = false;

		public AccelerationSensorEventListener(TextView outputView) {
			output = outputView;
			firstTime = 1;
			// sets state as 0 when app begins
		}

		private String direction(float angle) {
			if (angle < (float) -5 * Math.PI / 6) {
				return "South";
			} else if (angle < (float) -2 * Math.PI / 3) {
				return "South-West";
			} else if (angle < (float) -Math.PI / 3) {
				return "West";
			} else if (angle < (float) -Math.PI / 6) {
				return "North-West";
			} else if (angle < (float) Math.PI / 6) {
				return "North";
			} else if (angle < (float) Math.PI / 3) {
				return "North-East";
			} else if (angle < (float) 2 * Math.PI / 3) {
				return "East";
			} else if (angle < (float) 5 * Math.PI / 6) {
				return "South-East";
			} else {
				return "South";
			}
		}

		private void walkingDirection(float theta, float[] direction) {
			double angle = theta;
			if (direction.length != 2) {
				return;
			}

			// if (angle < (float) -5 * Math.PI / 6) {
			// direction[0] = (float) 0;
			// direction[1] = (float) -1;
			// } else if (angle < (float) -2 * Math.PI / 3) {
			// direction[0] = (float) -0.70710678;
			// direction[1] = (float) -0.70710678;
			// } else if (angle < (float) -Math.PI / 3) {
			// direction[0] = (float) -1;
			// direction[1] = (float) 0;
			// } else if (angle < (float) -Math.PI / 6) {
			// direction[0] = (float) -0.70710678;
			// direction[1] = (float) 0.70710678;
			// } else if (angle < (float) Math.PI / 6) {
			// direction[0] = (float) 0;
			// direction[1] = (float) 1;
			// } else if (angle < (float) Math.PI / 3) {
			// direction[0] = (float) 0.70710678;
			// direction[1] = (float) 0.70710678;
			// } else if (angle < (float) 2 * Math.PI / 3) {
			// direction[0] = (float) 1;
			// direction[1] = (float) 0;
			// } else if (angle < (float) 5 * Math.PI / 6) {
			// direction[0] = (float) 0.70710678;
			// direction[1] = (float) -0.70710678;
			// } else {
			// direction[0] = (float) 0;
			// direction[1] = (float) -1;
			// }

			direction[0] = (float) Math.sin(angle);
			direction[1] = (float) Math.cos(angle);
			return;
		}

		float[] lowpass(float[] in) {
			float[] out = new float[in.length];
			float a = 10;
			out[0] = 0;
			for (int i = 1; i < in.length; i++) {
				out[i] = a * in[i] + (1 - a) * out[i - 1];
			}
			return out;
			// low pass filter algorithim
		}

		public void onAccuracyChanged(Sensor s, int i) {
		}

		public void onSensorChanged(SensorEvent me) {

			if (me.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
				as2.setText("\nAccelerometer: \nX: " + df.format(me.values[0])
						+ "\nY: " + df.format(me.values[1]) + "\nZ: "
						+ df.format(me.values[2]));

				if (firstTime == 1) {
					state = 0;
					firstTime = 0;
				}

				switch (state) {
				case 0:
					if (lowpass(me.values)[2] > 5.00) {
						state = 1;
					}

				case 1:
					if (lowpass(me.values)[2] > 25.00 && state == 1) {
						state = 2;

					} else if (System.currentTimeMillis() - lastStep > 1000) {
						state = 0;
						// if it takes too long between current step and last
						// step, state will be reset to zero.
						break;
					}

				case 2:
					if (lowpass(me.values)[2] < -25.00 && state == 2) {
						if (System.currentTimeMillis() - lastStep < 300) {
							state = 0;
							// if too little time occurs between the current
							// step and last step, then it will not count as a
							// step
						} else {
							PointF checkWall = new PointF((float) (userPosition.x + 0.7 * Math.sin(theta)),
									(float) (userPosition.y - 0.7 * Math.cos(theta)));
							// If there is an obstacle in the way, does not update user Position
							if (clearPath(checkWall, userPosition)) {						
							
							stepCounter++;
							state = 0;
							positionFromStartArray[0] += positionArray[0];
							positionFromStartArray[1] += positionArray[1];
							displacement = (float) Math
									.sqrt(positionFromStartArray[0]
											* positionFromStartArray[0]
											+ positionFromStartArray[1]
											* positionFromStartArray[1]);

							updateUserPosition(userPosition, theta);
							}
							else{
								state = 0;
							}

							// If userPoint is at destinationPoint, inform the
							// user
							statusUpdate();
						}

						lastStep = System.currentTimeMillis();
						// sets the time of the laststep
						break;
					}
				}
				stepDisplay.setText("Steps: " + stepCounter);
				position.setText("Position From Start: \nX: "
						+ df.format(positionFromStartArray[0]) + "\nY: "
						+ df.format(positionFromStartArray[1])
						+ "\nDisplacement: " + displacement);

				graph.addPoint(me.values);
			}

			if (me.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				gravMatrix = me.values;
			}
			if (me.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				geoMagMatrix = me.values;
				if (gravMatrix != null && geoMagMatrix != null) {
					float I[] = new float[9];
					SensorManager.getRotationMatrix(mRotation, I, gravMatrix,
							geoMagMatrix);
					SensorManager.getOrientation(mRotation, mOrientation);

					theta = mOrientation[0];
					if (previousAngle == 0) {
						previousAngle = mOrientation[0];
						currentAngle = mOrientation[0];
					} else {
						currentAngle = (previousAngle + calibrate + mOrientation[0]) / 2;
						previousAngle = currentAngle;
					}

					String direction = direction(currentAngle);

					walkingDirection(currentAngle, positionArray);

					mOrientationData.setText("\nCurrent Direction:" + direction
							+ "\nTEST: " + mOrientation[0]);
				}
			}
		}
	}

	class Location implements PositionListener {
		PointF origin;
		PointF destination;

		@Override
		public void originChanged(MapView source, PointF loc) {
			// TODO Auto-generated method stub
			// Sets userPoint and OriginPoint
			source.setUserPoint(loc);
			source.setOriginPoint(loc);
			if (firstTry == 1) {
				updateUserPosition(userPosition, 0);
				updateUserPosition(userPosition, (float) 3.141592654);
			}
			origin = loc;
			firstTry = 1;
		}

		@Override
		public void destinationChanged(MapView source, PointF dest) {
			// TODO Auto-generated method stub
			// Sets Destination Point and draws a line between the origin and
			// destination
			source.setDestinationPoint(dest);
			path.add(dest);
			drawPath(origin, dest);
			mapView.setUserPath(path);
			path.clear();
			destination = dest;

			if (firstTry == 1) {
				updateUserPosition(userPosition, 0);
				updateUserPosition(userPosition, (float) 3.141592654);
			}

			firstTry = 1;
			statusUpdate();
		}
	}

	public void drawPath(PointF start, PointF finish) {
		// If no obstacles exist between start and finish, then draw a line
		// between the two points

		if (clearPath(start, finish)) {
			path.add(start);
			path.add(finish);
		} else {
			path.add(start);
			// list of intercepts from start to destination point
			List<InterceptPoint> intercepts = map1.calculateIntersections(
					start, finish);
			PointF currentPoint = start;

			// loop through intercept list
			for (int i = 0; i < intercepts.size(); i++) {
				PointF intersectionPoint = intercepts.get(i).getPoint();
				float yIntersect = intersectionPoint.y;

				// while there is obstruction between user position and intercept,
				// shift up or down the y coordinate
				while (!clearPath(currentPoint, intersectionPoint)) {
					if (yIntersect < 10) {
						intersectionPoint.y += 0.1;
					} else
						intersectionPoint.y -= 0.1;
				}
				currentPoint = intersectionPoint;

				// add the fixed intercept point to the path
				path.add(currentPoint);
				intercepts.remove(i);
			}
		}

		getInstructions();
	}

	public void updateUserPosition(PointF current, float angle) {
		nextStep = new PointF((float) (current.x + 0.7 * Math.sin(angle)),
				(float) (current.y - 0.7 * Math.cos(angle)));
		// If there is an obstacle in the way, does not update user Position
		if (clearPath(nextStep, current)) {
			mapView.setUserPoint(nextStep);
		}
		// Updates and redraws path everytime a step is taken
		drawPath(mapView.getUserPoint(), mapView.getDestinationPoint());
		path.add(mapView.getDestinationPoint());
		mapView.setUserPath(path);
		// Clears Old path
		path.clear();
	}

	// Checks if there is an obstruction between point 1 and point 2
	public boolean clearPath(PointF point1, PointF point2) {
		if (map1.calculateIntersections(point1, point2).isEmpty()) {
			return true;
		} else
			return false;
	}

	public void statusUpdate() {
		float distanceX = mapView.getUserPoint().x
				- mapView.getDestinationPoint().x;
		float distanceY = mapView.getUserPoint().y
				- mapView.getDestinationPoint().y;
		float distance = (float) Math.sqrt(Math.pow(distanceX, 2)
				+ Math.pow(distanceY, 2));

		if (Math.abs(mapView.getUserPoint().x - mapView.getDestinationPoint().x) < 0.3
				&& Math.abs(mapView.getUserPoint().y
						- mapView.getDestinationPoint().y) < 0.3) {
			status = "You are here!";
		} else {
			status = "Keep Walking \n You are " + distance + "away.";
		}

		arrived.setText(status);
	}

	public void calibrateNorth() {
		calibrate = -mOrientation[0];
	}

	public void getInstructions() {

		if (mapView.getUserPoint().x > mapView.getDestinationPoint().x) {
			eastWest = "west";
		} else {
			eastWest = "east";
		}

		if (mapView.getUserPoint().y > mapView.getDestinationPoint().y) {
			northSouth = "North";
		} else {
			northSouth = "South";
		}

		stepsX = Math.abs(Math.round((mapView.getUserPoint().x - mapView
				.getDestinationPoint().x) * 1.4285));
		stepsY = Math.abs(Math.round((mapView.getUserPoint().y - mapView
				.getDestinationPoint().y) * 1.4285));

		instructions.setText("Go " + stepsY + " steps " + northSouth + " and "
				+ stepsX + " steps " + eastWest);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

}
