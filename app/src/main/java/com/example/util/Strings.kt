package com.example.util

enum class AppLanguage(val code: String, val displayName: String) {
    HINDI("hi", "हिंदी"),
    ENGLISH("en", "English")
}

object AppStrings {
    private val strings = mapOf(
        "app_title" to ("KGN Construction" to "केजीएन कंस्ट्रक्शन"),
        "app_subtitle" to ("Owner Attendance & Payroll Management" to "मालिक उपस्थिति एवं मजदूरी प्रबंधन"),
        "home" to ("Home" to "होम"),
        "attendance" to ("Attendance" to "हाज़िरी"),
        "workers" to ("Workers" to "मज़दूर"),
        "sites" to ("Sites" to "साइट्स"),
        "wages" to ("Wages" to "वेतन"),
        "reports" to ("Reports" to "रिपोर्ट्स"),
        "settings" to ("Settings" to "सेटिंग्स"),
        "advances" to ("Advances" to "एडवांस"),
        "payments" to ("Payments" to "भुगतान"),

        // Dashboard Metrics
        "active_workers" to ("Active Workers" to "सक्रिय मज़दूर"),
        "present_today" to ("Present Today" to "आज उपस्थित"),
        "absent_today" to ("Absent Today" to "आज अनुपस्थित"),
        "half_day" to ("Half Day" to "आधा दिन (0.5)"),
        "active_sites" to ("Active Sites" to "चालू साइट्स"),
        "today_cost" to ("Today's Labour Cost" to "आज का कुल खर्च"),
        "month_earned" to ("Month Wages" to "इस महीने का वेतन"),
        "total_advances" to ("Pending Advances" to "बकाया एडवांस"),
        "unpaid_wages" to ("Unpaid Wages Balance" to "बकाया मज़दूरी"),

        // Quick Actions
        "quick_actions" to ("Quick Actions" to "त्वरित कार्य"),
        "action_mark_att" to ("Mark Attendance" to "हाज़िरी लगाएं"),
        "action_add_worker" to ("Add Worker" to "नया मज़दूर जोड़ें"),
        "action_add_site" to ("Add Site" to "नई साइट जोड़ें"),
        "action_record_payment" to ("Record Payment" to "मज़दूरी भुगतान"),
        "action_record_advance" to ("Give Advance" to "एडवांस / पेशगी"),
        "action_view_reports" to ("View Reports" to "रिपोर्ट्स देखें"),

        // Attendance Screen
        "select_site" to ("Select Construction Site" to "साइट चुनें"),
        "all_sites" to ("All Sites" to "सभी साइट्स"),
        "mark_all_present" to ("Mark All Present" to "सबको उपस्थित करें"),
        "save_attendance" to ("Save Attendance" to "हाज़िरी सेव करें"),
        "attendance_saved" to ("Attendance saved successfully!" to "हाज़िरी सफलतापूर्वक सेव हो गई!"),
        "duplicate_warning" to ("Attendance already exists for this date. Changes updated." to "इस तारीख की हाज़िरी पहले से दर्ज थी, अपडेट कर दी गई।"),
        "overtime_hours" to ("OT Hours" to "ओवरटाइम घंटे"),
        "present" to ("Present" to "उपस्थित"),
        "absent" to ("Absent" to "अनुपस्थित"),
        "leave" to ("Paid Leave" to "सवेतन छुट्टी"),
        "weekly_off" to ("Weekly Off" to "साप्ताहिक अवकाश"),

        // Worker Fields
        "worker_name" to ("Worker Name" to "मज़दूर का नाम"),
        "mobile_no" to ("Mobile Number" to "मोबाइल नंबर"),
        "work_category" to ("Category / Skill" to "काम की श्रेणी"),
        "daily_wage" to ("Daily Wage (₹/day)" to "दैनिक दिहाड़ी (₹/दिन)"),
        "ot_rate" to ("OT Rate (₹/hr)" to "ओवरटाइम दर (₹/घंटा)"),
        "joining_date" to ("Joining Date" to "जुड़ने की तारीख"),
        "assigned_site" to ("Assigned Site" to "तैनात साइट"),
        "notes" to ("Notes / Remarks" to "टिप्पणी / रिमार्क्स"),
        "active" to ("Active" to "सक्रिय"),
        "inactive" to ("Inactive" to "निष्क्रिय"),
        "save_worker" to ("Save Worker" to "मज़दूर सेव करें"),

        // Worker Profile & Ledger
        "worker_profile" to ("Worker Profile & Ledger" to "मज़दूर प्रोफ़ाइल व खाता"),
        "total_earned" to ("Total Earned" to "कुल कमाई"),
        "total_advances_given" to ("Advances Given" to "कुल एडवांस दिया"),
        "total_paid" to ("Total Paid" to "कुल भुगतान किया"),
        "balance_due" to ("Outstanding Balance" to "बाकी शेष राशि"),
        "make_payment" to ("Pay Wages" to "मज़दूरी दें"),
        "give_advance" to ("Give Advance" to "एडवांस दें"),
        "share_whatsapp" to ("Share on WhatsApp" to "व्हाट्सएप पर शेयर करें"),
        "call_worker" to ("Call Worker" to "कॉल करें"),

        // Payment & Advance Vouchers
        "payment_voucher" to ("Payment Voucher / Receipt" to "भुगतान रसीद"),
        "advance_voucher" to ("Advance Voucher" to "एडवांस रसीद"),
        "payment_mode" to ("Payment Method" to "भुगतान माध्यम"),
        "cash" to ("Cash" to "नकद (Cash)"),
        "upi" to ("UPI (GPay / PhonePe)" to "यूपीआई (UPI)"),
        "bank_transfer" to ("Bank Transfer" to "बैंक ट्रांसफर"),
        "reference_no" to ("Reference / UTR No." to "रेफरेंस नंबर"),
        "amount_inr" to ("Amount (₹)" to "रकम (₹)"),
        "save_transaction" to ("Save Transaction" to "लेनदेन सेव करें"),

        // Construction Sites
        "site_name" to ("Site Name" to "साइट का नाम"),
        "site_address" to ("Location / Address" to "स्थान / पता"),
        "start_date" to ("Start Date" to "शुरू होने की तारीख"),
        "completion_date" to ("Completion Date" to "पूरा होने की तारीख"),
        "site_status" to ("Status" to "स्थिति"),
        "completed" to ("Completed" to "पूर्ण"),
        "save_site" to ("Save Site" to "साइट सेव करें"),

        // Owner Auth
        "owner_login" to ("Owner Login" to "मालिक लॉगिन"),
        "enter_pin" to ("Enter 4-Digit Owner PIN" to "4-अंकों का मालिक पिन दर्ज करें"),
        "default_pin_hint" to ("Default PIN is 1234. Change it anytime in Settings." to "डिफ़ॉल्ट पिन 1234 है। सेटिंग्स में कभी भी बदलें।"),
        "unlock" to ("Unlock" to "अनलॉक करें"),
        "incorrect_pin" to ("Incorrect PIN. Please try again." to "गलत पिन। कृपया दोबारा प्रयास करें।"),
        "lock_app" to ("Lock App" to "ऐप लॉक करें"),

        // Reports
        "export_csv" to ("Export CSV" to "CSV निर्यात"),
        "share_report" to ("Share Report" to "रिपोर्ट शेयर करें"),
        "date_range" to ("Date Range" to "तारीख दायरा"),
        "this_week" to ("This Week" to "इस सप्ताह"),
        "this_month" to ("This Month" to "इस महीने"),
        "all_time" to ("All Time" to "पूरा रिकॉर्ड"),

        // Empty States
        "no_workers_found" to ("No workers added yet. Tap below to add your first worker." to "कोई मज़दूर नहीं जुड़ा। पहला मज़दूर जोड़ने के लिए नीचे टैप करें।"),
        "no_sites_found" to ("No sites added yet. Add your first construction project." to "कोई साइट नहीं जुड़ी। अपनी पहली कंस्ट्रक्शन साइट जोड़ें।"),
        "no_attendance_records" to ("No attendance marked for this date." to "इस तारीख की कोई हाज़िरी दर्ज नहीं है।"),
        "no_transactions" to ("No transactions recorded yet." to "अभी तक कोई लेन-देन दर्ज नहीं है।")
    )

    fun get(key: String, language: AppLanguage): String {
        val pair = strings[key] ?: return key
        return if (language == AppLanguage.HINDI) pair.second else pair.first
    }
}
