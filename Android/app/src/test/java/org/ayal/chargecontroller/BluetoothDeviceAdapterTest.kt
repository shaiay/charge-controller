package org.ayal.chargecontroller

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.ayal.chargecontroller.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BluetoothDeviceAdapterTest {

    // Mocks for getItemCount
    @Mock private lateinit var mockBluetoothDevice1: BluetoothDevice
    @Mock private lateinit var mockBluetoothDevice2: BluetoothDevice
    @Mock private lateinit var mockBluetoothDevice3: BluetoothDevice

    // Mocks for onBindViewHolder
    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockPackageManager: PackageManager
    @Mock private lateinit var mockView: View // Mock for general view interactions, e.g. ViewHolder's itemView for onBind
    @Mock private lateinit var mockDeviceNameTextView: TextView
    @Mock private lateinit var mockDeviceAddressTextView: TextView
    @Mock private lateinit var mockBluetoothDevice: BluetoothDevice // Single device for onBindViewHolder tests
    @Mock private lateinit var mockOnItemClicked: (BluetoothDevice) -> Unit
    @Mock private lateinit var mockViewHolder: BluetoothDeviceAdapter.ViewHolder // Mock for onBindViewHolder

    // Mocks for onCreateViewHolder
    @Mock private lateinit var mockViewGroup: ViewGroup
    @Mock private lateinit var mockLayoutInflater: LayoutInflater
    @Mock private lateinit var mockInflatedView: View // This is the view returned by inflater

    private var layoutInflaterMockedStatic: MockedStatic<LayoutInflater>? = null

    @Before
    fun setUp() {
        // Common mocks for onBindViewHolder
        `when`(mockViewHolder.itemView).thenReturn(mockView)
        `when`(mockView.context).thenReturn(mockContext) // General context for mockView
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)

        // Since mockViewHolder is a direct mock, we stub its properties directly for onBindViewHolder tests.
        `when`(mockViewHolder.deviceNameTextView).thenReturn(mockDeviceNameTextView)
        `when`(mockViewHolder.deviceAddressTextView).thenReturn(mockDeviceAddressTextView)

        // Setup for onCreateViewHolder
        `when`(mockViewGroup.context).thenReturn(mockContext) // Context for LayoutInflater.from

        // Static mock for LayoutInflater
        // Check if Mockito version supports this way of static mocking.
        // This requires mockito-inline dependency or equivalent setup.
        layoutInflaterMockedStatic = Mockito.mockStatic(LayoutInflater::class.java).apply {
            `when`(LayoutInflater.from(mockContext)).thenReturn(mockLayoutInflater)
        }

        `when`(mockLayoutInflater.inflate(R.layout.item_bluetooth_device, mockViewGroup, false)).thenReturn(mockInflatedView)
        `when`(mockInflatedView.findViewById<TextView>(R.id.deviceNameTextView)).thenReturn(mockDeviceNameTextView)
        `when`(mockInflatedView.findViewById<TextView>(R.id.deviceAddressTextView)).thenReturn(mockDeviceAddressTextView)
    }

    @After
    fun tearDown() {
        layoutInflaterMockedStatic?.close()
    }

    @Test
    fun onCreateViewHolder_inflatesViewAndReturnsViewHolder() {
        // Arrange
        val devices = emptyList<BluetoothDevice>() // Not used by onCreateViewHolder itself
        val adapter = BluetoothDeviceAdapter(devices, mockOnItemClicked)

        // Act
        val createdViewHolder = adapter.onCreateViewHolder(mockViewGroup, 0) // ViewType is 0

        // Assert
        // Verify static method was called via the MockedStatic reference if possible, or directly.
        // Verification of static method is tricky with `Mockito.verify` on the class.
        // Instead, we verify the interactions that follow from it.
        verify(mockLayoutInflater).inflate(R.layout.item_bluetooth_device, mockViewGroup, false)
        assertNotNull(createdViewHolder)
        assertEquals(mockInflatedView, createdViewHolder.itemView)
        // Also verify that the ViewHolder's internal text views would be correctly assigned
        // if it were a real ViewHolder. This is implicitly tested by the mocks for findViewById on mockInflatedView
        // which are used by the ViewHolder's constructor.
        assertEquals(mockDeviceNameTextView, createdViewHolder.deviceNameTextView)
        assertEquals(mockDeviceAddressTextView, createdViewHolder.deviceAddressTextView)
    }

    @Test
    fun getItemCount_returnsCorrectSize() {
        val devices = listOf(mockBluetoothDevice1, mockBluetoothDevice2, mockBluetoothDevice3)
        // It's better to use the real onItemClicked lambda if it's simple or mock it if complex
        val adapter = BluetoothDeviceAdapter(devices, mockOnItemClicked)
        assertEquals(devices.size, adapter.itemCount)
    }

    @Test
    fun onBindViewHolder_permissionGranted_setsNameAndAddress() {
        // Arrange
        val deviceName = "Test Device"
        val deviceAddress = "00:11:22:33:44:55"
        val devices = listOf(mockBluetoothDevice)
        val adapter = BluetoothDeviceAdapter(devices, mockOnItemClicked)

        // Mocking device properties
        // For SDK < S, name is directly accessed.
        // We need to control Build.VERSION.SDK_INT for the permission check branch.
        // Let's modify Build.VERSION.SDK_INT for this test.
        // This is tricky. A common way is to use Robolectric or wrap Build.VERSION.SDK_INT calls.
        // For a pure Mockito test, we can't change static final fields like SDK_INT.
        // The code is: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission( ... ) != PackageManager.PERMISSION_GRANTED)
        // So, if we ensure checkSelfPermission returns PERMISSION_GRANTED, the first part of the condition doesn't prevent access.

        `when`(mockContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)).thenReturn(PackageManager.PERMISSION_GRANTED)
        // The following is needed if we are testing the "name not available" branch, but here we assume name is available.
        // `when`(mockBluetoothDevice.name).thenReturn(deviceName) // This will cause SecurityException if BLUETOOTH_CONNECT is not granted on API >= S
        // `when`(mockBluetoothDevice.address).thenReturn(deviceAddress)

        // To test the "permission granted path" for name, we need to mock the @SuppressLint("MissingPermission") part.
        // The adapter code will call device.name and device.address.
        // If permission is granted, these calls should succeed.
        // If we mock checkSelfPermission to return PERMISSION_GRANTED, then device.name should be called.
        `when`(mockBluetoothDevice.name).thenReturn(deviceName)
        `when`(mockBluetoothDevice.address).thenReturn(deviceAddress)


        // When onBindViewHolder is called, it will internally try to get deviceNameTextView and deviceAddressTextView
        // from the ViewHolder. Our setUp method has already configured mockViewHolder.itemView.findViewById.
        // However, the adapter's onBindViewHolder takes a ViewHolder created by its onCreateViewHolder.
        // So, we need to ensure the ViewHolder passed to onBindViewHolder behaves as if its views are our mocks.
        // The current mockViewHolder passed to onBindViewHolder is a direct mock.
        // We need to ensure its deviceNameTextView and deviceAddressTextView properties return our mocks.
        // This requires that BluetoothDeviceAdapter.ViewHolder has these as accessible properties.
        // Let's assume they are:
        `when`(mockViewHolder.deviceNameTextView).thenReturn(mockDeviceNameTextView)
        `when`(mockViewHolder.deviceAddressTextView).thenReturn(mockDeviceAddressTextView)


        // Act
        adapter.onBindViewHolder(mockViewHolder, 0)

        // Assert
        verify(mockDeviceNameTextView).text = deviceName
        verify(mockDeviceAddressTextView).text = deviceAddress
        verify(mockViewHolder.itemView).setOnClickListener(any(View.OnClickListener::class.java))
    }

    @Test
    fun onBindViewHolder_permissionDeniedApiSPlus_setsPermissionDeniedText() {
        // Arrange
        val deviceAddress = "00:11:22:AA:BB:CC"
        val devices = listOf(mockBluetoothDevice)
        val adapter = BluetoothDeviceAdapter(devices, mockOnItemClicked)

        // Simulate API S+ and permission denied
        // As discussed, controlling Build.VERSION.SDK_INT is tricky.
        // This test relies on the test execution environment being S+ or that
        // the SUT's check for SDK_INT allows this path if checkSelfPermission is DENIED.
        // For the purpose of this test, we assume the SUT will check permission if SDK_INT >= S.
        // We ensure checkSelfPermission returns DENIED. The SUT should then show "Permission Denied".
        `when`(mockContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)).thenReturn(PackageManager.PERMISSION_DENIED)
        // If Build.VERSION.SDK_INT in test environment is < S, this test case might not behave as intended
        // as the first part of `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ...)` would be false.

        `when`(mockBluetoothDevice.address).thenReturn(deviceAddress)
        // mockBluetoothDevice.name should not be called if permission is denied for BLUETOOTH_CONNECT on S+

        // Act
        adapter.onBindViewHolder(mockViewHolder, 0) // Assuming holder's views are already mocked in setup

        // Assert
        // This assertion depends on Build.VERSION.SDK_INT being >= Build.VERSION_CODES.S in the test environment
        // or the SUT's logic being structured such that "Permission Denied" is set based on this mock.
        // For now, we proceed with the assumption that the condition for setting "Permission Denied"
        // will be met with checkSelfPermission returning PERMISSION_DENIED on an S+ like check.
        verify(mockDeviceNameTextView).text = "Permission Denied"
        verify(mockDeviceAddressTextView).text = deviceAddress
        verify(mockViewHolder.itemView).setOnClickListener(any(View.OnClickListener::class.java))
    }

    @Test
    fun onBindViewHolder_nullDeviceName_setsUnknownDeviceText() {
        // Arrange
        val deviceAddress = "00:11:22:CC:DD:EE"
        val devices = listOf(mockBluetoothDevice)
        val adapter = BluetoothDeviceAdapter(devices, mockOnItemClicked)

        // Ensure permission is granted (or SDK < S) so that device.name is accessed
        `when`(mockContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)).thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(mockBluetoothDevice.name).thenReturn(null) // Key part of this test
        `when`(mockBluetoothDevice.address).thenReturn(deviceAddress)

        // Act
        adapter.onBindViewHolder(mockViewHolder, 0)

        // Assert
        verify(mockDeviceNameTextView).text = "Unknown Device"
        verify(mockDeviceAddressTextView).text = deviceAddress
        verify(mockViewHolder.itemView).setOnClickListener(any(View.OnClickListener::class.java))
    }

    @Test
    fun onBindViewHolder_itemClick_invokesCallback() {
        // Arrange
        val devices = listOf(mockBluetoothDevice)
        val adapter = BluetoothDeviceAdapter(devices, mockOnItemClicked)
        val listenerCaptor = ArgumentCaptor.forClass(View.OnClickListener::class.java)

        // Standard setup for name/address to be displayed - ensure permission granted
        `when`(mockContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)).thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(mockBluetoothDevice.name).thenReturn("Test Device")
        `when`(mockBluetoothDevice.address).thenReturn("00:11:22:DD:EE:FF")

        // Act
        adapter.onBindViewHolder(mockViewHolder, 0)

        // Capture the listener
        verify(mockViewHolder.itemView).setOnClickListener(listenerCaptor.capture())
        listenerCaptor.value.onClick(mockView) // Simulate click

        // Assert
        verify(mockOnItemClicked).invoke(mockBluetoothDevice)
    }
}
