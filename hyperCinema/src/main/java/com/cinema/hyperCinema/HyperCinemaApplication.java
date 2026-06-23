package com.cinema.hyperCinema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HyperCinemaApplication {

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "false");
		SpringApplication.run(HyperCinemaApplication.class, args);
		System.out.println("=================================================");
		System.out.println("HyperCinema started successfully!");
		System.out.println("Access local site: http://localhost:8080");
		System.out.println("=================================================");

		try {
			openBrowser("http://localhost:8080");
		} catch (Exception e) {
			System.out.println("Tự động mở trình duyệt thất bại: " + e.getMessage());
		}
	}

	private static void openBrowser(String url) {
		String os = System.getProperty("os.name").toLowerCase();
		try {
			if (java.awt.Desktop.isDesktopSupported()) {
				java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
				return;
			}
		} catch (Exception e) {
			System.out.println("Desktop.browse không hoạt động, thử cách khác...");
		}

		// Fallback for different OS using command line
		try {
			if (os.contains("win")) {
				Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
			} else if (os.contains("mac")) {
				Runtime.getRuntime().exec(new String[]{"open", url});
			} else if (os.contains("nix") || os.contains("nux")) {
				Runtime.getRuntime().exec(new String[]{"xdg-open", url});
			} else {
				System.out.println("Không xác định được hệ điều hành để tự động mở trình duyệt: " + os);
			}
		} catch (Exception e) {
			System.out.println("Mở trình duyệt qua dòng lệnh thất bại: " + e.getMessage());
		}
	}

}
