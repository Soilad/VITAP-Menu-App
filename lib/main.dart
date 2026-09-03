import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:intl/intl.dart';


class TimeEntry {
  String   name  = "";
  int      id    = 0;
  DateTime start = DateTime.now();
  DateTime end   = DateTime.now();
  TimeEntry(
      this.name,
      this.id,
      this.start,
      this.end,
    );

  String display() {
    return "$name (${DateFormat.jm().format(start)} - ${DateFormat.jm().format(end)})";
  }
}

class FoodType {
  String emoji = "";
  Color color  = Colors.white;
  FoodType(
    this.emoji,
    this.color,
  );
}

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'VITAP Mess Menu',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // TRY THIS: Try running your application with "flutter run". You'll see
        // the application has a purple toolbar. Then, without quitting the app,
        // try changing the seedColor in the colorScheme below to Colors.green
        // and then invoke "hot reload" (save your changes or press the "hot
        // reload" button in a Flutter-supported IDE, or press "r" if you used
        // the command line to start the app).
        //
        // Notice that the counter didn't reset back to zero; the application
        // state is not lost during the reload. To reset the state, use hot
        // restart instead.
        //
        // This works for code too, not just values: Most code changes can be
        // tested with just a hot reload.
        colorScheme: ColorScheme.fromSeed(
            seedColor: Colors.red,
            brightness: Brightness.dark,
            contrastLevel: 0.5,
        ),
      ),
      home: const MyHomePage(title: 'VIT-AP Mess Menu'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  List<Widget>? periodWidgets = [];
  DateTime _selectedDate = DateTime.now();
  late final Future<List<dynamic>> futureMenuJSON = getJson();
  var timeEntries = <TimeEntry>[
    TimeEntry(
      "Breakfast",
      0,
      DateTime(0, 1, 1, 7, 15),
      DateTime(0, 1, 1, 9, 0),
    ), TimeEntry(
      "Lunch",
      1,
      DateTime(0, 1, 1, 12, 15),
      DateTime(0, 1, 1, 14, 0),
    ), TimeEntry(
      "Snacks",
      2,
      DateTime(0, 1, 1, 16, 15),
      DateTime(0, 1, 1, 18, 15),
    ), TimeEntry(
      "Dinner",
      3,
      DateTime(0, 1, 1, 19, 15),
      DateTime(0, 1, 1, 21, 0),
    ),
  ];

  Future<List<dynamic>> getJson() async {
    final SharedPreferences prefs = await SharedPreferences.getInstance();
    final httpPackageUrl = Uri.https('soilad.github.io', '/VITAP-Menu/menu.json');

    try {
      final httpPackageInfo = await http.read(httpPackageUrl);
      await prefs.setString('menu_json', httpPackageInfo);
      final httpPackageJson = json.decode(httpPackageInfo) as List<dynamic>;
      return httpPackageJson;
    } catch (e) {
      final String? cachedJson = prefs.getString('menu_json');
      if (cachedJson != null) {
        return json.decode(cachedJson) as List<dynamic>;
      }
      rethrow;
    }
  }

  Future<void> _changeDate() async {
    DateTime now   = DateTime.now();
    DateTime start = DateTime(now.year, now.month, 1);
    DateTime end   = DateTime(now.year, now.month + 1, 0);
    final DateTime? pickedDate = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate:   start,
      lastDate:    end,
    );
    if (pickedDate != null) {
      setState(
        () {
          _selectedDate = pickedDate;
          _updateByDay(timeEntries, _selectedDate);
        }
      );
    }

  }

  @override
  void initState() {
    super.initState();
    _updateByDay(timeEntries, _selectedDate);
    _updateByTime(timeEntries, _selectedDate);
  }

  void _updateByTime(List<TimeEntry> timeEntries, DateTime selectedDate) async {
    timeEntries.sort(
      (a, b) {
        final now = DateTime.now();
        return (
          (a.start.hour - now.hour).abs().compareTo((b.start.hour - now.hour).abs())
        );
      }
    );
  }

  void _updateByDay(List<TimeEntry> timeEntries, DateTime selectedDate) async {
    List<dynamic> menuJSON = await futureMenuJSON;
    setState(
      () {
        int currentMonthDay = (selectedDate.day - 1) % 14;
        periodWidgets = [
          Container(
            margin: EdgeInsets.all(10),
            child: ActionChip(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.all(
                  Radius.circular(40)
                )
              ),
              onPressed: _changeDate,
              label: SizedBox(
                width: double.infinity,
                child: Text(
                  style: const TextStyle(
                    fontStyle: FontStyle.italic,
                    fontSize: 28,
                  ),
                  textAlign: TextAlign.center,
                  DateFormat('dd-MM-yyyy')
                  .format(_selectedDate)
                )
              )
            )
          )
        ];

        final List<FoodType> foodEntryColor = [
          FoodType("🥦", Theme.of(context).colorScheme.onSecondary),
          FoodType("🍗", Theme.of(context).colorScheme.onPrimary),
          FoodType("✨🥦", Theme.of(context).colorScheme.onSecondaryFixed),
          FoodType("✨🍗", Theme.of(context).colorScheme.onPrimaryFixed),
        ];

        List<dynamic> periods = menuJSON[currentMonthDay];
        for (var timeEntry in timeEntries) {
          List<dynamic> period = periods[timeEntry.id];
          periodWidgets?.add(
            Container(
              margin: EdgeInsets.all(10),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(20),
                color: Theme.of(context).colorScheme.onPrimaryContainer
              ),
              padding: EdgeInsets.all(10),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                      timeEntry.display(),
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 28,
                      )
                  ),
                  Wrap(
                    runSpacing: 10,
                    spacing: 10,
                    children: period.map((foodEntry) => Chip(
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(20.0), // change this value
                        side: BorderSide(color: Colors.black45, width: 0.0),
                      ),
                      label: Text(
                        foodEntryColor[foodEntry["type"]].emoji + foodEntry["name"],
                        style: TextStyle(
                          color: Colors.white,
                        ),
                      ),
                      backgroundColor: foodEntryColor[foodEntry["type"]].color,
                    )
                    ).toList(),
                  )
                ],
              ),
            )
          );
        }
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    // This method is rerun every time setState is called, for instance as done
    // by the _incrementCounter method above.
    //
    // The Flutter framework has been optimized to make rerunning build methods
    // fast, so that you can just rebuild anything that needs updating rather
    // than having to individually change instances of widgets.
    // DateTime now = DateTime.now();
    // DateTime start = DateTime(now.year, now.month, 1);
    // DateTime end = DateTime(now.year, now.month + 1, 1);
    return Scaffold(
      appBar: AppBar(
        // TRY THIS: Try changing the color here to a specific color (to
        // Colors.amber, perhaps?) and trigger a hot reload to see the AppBar
        // change color while the other colors stay the same.
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        // Here we take the value from the MyHomePage object that was created by
        // the App.build method, and use it to set our appbar title.
        title: Text(widget.title),
      ),
      body: RefreshIndicator(
        // Center is a layout widget. It takes a single child and positions it
        // in the middle of the parent.
        onRefresh: () async {
          _updateByTime(timeEntries, _selectedDate);
        },
        child: ListView(
          // padding: EdgeInsets.all(5),
          // Column is also a layout widget. It takes a list of children and
          // arranges them vertically. By default, it sizes itself to fit its
          // children horizontally, and tries to be as tall as its parent.
          //
          // Column has various properties to control how it sizes itself and
          // how it positions its children. Here we use mainAxisAlignment to
          // center the children vertically; the main axis here is the vertical
          // axis because Columns are vertical (the cross axis would be
          // horizontal).
          //
          // TRY THIS: Invoke "debug painting" (choose the "Toggle Debug Paint"
          // action in the IDE, or press "p" in the console), to see the
          // wireframe for each widget.
          // mainAxisAlignment: .center,
          children: periodWidgets ?? [],
          // children: [
          //   CalendarDatePicker(
          //     firstDate: start,
          //     initialDate: now,
          //     lastDate: end,
          //     onDateChanged: (selectedDate) {
          //       setState(
          //         () {
          //           update(periodWidgets, json as List<dynamic>, selectedDate);
          //         }
          //       );
          //     }
          //   )
          // ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _changeDate,
        tooltip: 'Select Date',
        child: const Icon(Icons.calendar_month),
      ),
    );
  }
}
