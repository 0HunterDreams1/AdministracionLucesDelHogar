#include <ESP8266WiFi.h>

const char* ssid = "Piper-2.4G";
const char* password = "QwerTy#123987";

WiFiServer server(80);

// Mapeo de IDs a pines GPIO
const int numLeds = 5;
int ledPins[numLeds] = {5, 4, 0, 14, 12}; // D1, D2, D3, D5, D6

void setup() {
  Serial.begin(115200);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  server.begin();
  Serial.println("\nWiFi conectado");
  Serial.print("IP: ");
  Serial.println(WiFi.localIP());

  // Inicializar pines como salida y apagar LEDs
  for (int i = 0; i < numLeds; i++) {
    pinMode(ledPins[i], OUTPUT);
    digitalWrite(ledPins[i], LOW); // apagado (activo bajo)
    Serial.println("LED apagado "+ String(i));
  }
}

void loop() {
  WiFiClient client = server.available();
  if (!client) return;

  unsigned long startTime = millis(); // ⏱ inicio

  // Esperar datos del cliente con timeout
  unsigned long timeout = millis();
  while (!client.available()) {
    if (millis() - timeout > 1000) {
      Serial.println("⏳ Timeout esperando datos del cliente");
      client.stop();
      return;
    }
  }

  // Leer la primera línea (request line)
  String requestLine = client.readStringUntil('\r');
  client.read(); // consumir '\n'
  requestLine.trim();
  Serial.println("📥 Request: " + requestLine);

  // Descartar headers
  while (client.available()) {
    String headerLine = client.readStringUntil('\r');
    client.read(); // consumir '\n'
    if (headerLine.length() == 0) break;
  }

  // Extraer path
  String path;
  int firstSpace = requestLine.indexOf(' ');
  int secondSpace = requestLine.indexOf(' ', firstSpace + 1);
  if (firstSpace != -1 && secondSpace != -1) {
    path = requestLine.substring(firstSpace + 1, secondSpace);
  }

  // Extraer parámetro ids=5,4,16
  int idsPos = path.indexOf("ids=");
  if (idsPos != -1) {
    String idsStr = path.substring(idsPos + 4);
    int ampPos = idsStr.indexOf('&');
    if (ampPos != -1) idsStr = idsStr.substring(0, ampPos);

    // Separar por comas y procesar cada GPIO
    while (idsStr.length() > 0) {
      int commaPos = idsStr.indexOf(',');
      String gpioStr;
      if (commaPos != -1) {
        gpioStr = idsStr.substring(0, commaPos);
        idsStr = idsStr.substring(commaPos + 1);
      } else {
        gpioStr = idsStr;
        idsStr = "";
      }

      gpioStr.trim();
      gpioStr.replace("\"", ""); // por si viene con comillas
      int gpio = gpioStr.toInt();

      // Validar GPIO (opcional)
      if (gpio >= 0 && gpio <= 16) {
        pinMode(gpio, OUTPUT); // asegurarse que esté configurado
        if (path.indexOf("/led/on") != -1) {
          digitalWrite(gpio, HIGH);
          Serial.println("💡 Encendiendo GPIO " + String(gpio));
        } else if (path.indexOf("/led/off") != -1) {
          digitalWrite(gpio, LOW);
          Serial.println("💡 Apagando GPIO " + String(gpio));
        }
      } else {
        Serial.println("⚠️ GPIO inválido: " + String(gpio));
      }
    }
  }


  // Respuesta HTTP
  const char* body = "OK";
  client.print("HTTP/1.1 200 OK\r\n");
  client.print("Content-Type: text/plain\r\n");
  client.print("Content-Length: ");
  client.print(strlen(body));
  client.print("\r\nConnection: close\r\n\r\n");
  client.print(body);
  client.flush();
  delay(10);
  client.stop();

  unsigned long endTime = millis(); // ⏱ fin
  Serial.println("✅ Solicitud procesada en " + String(endTime - startTime) + " ms");
}