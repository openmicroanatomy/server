<template id="dashboard">
	<div>
		<h1>OpenMicroanatomy Dashboard</h1>
		<ul>
			<li><b>Users:</b> {{ $javalin.state.Users }}</li>
			<li><b>Slides:</b> {{ $javalin.state.Slides }}</li>
			<li><b>Organizations:</b> {{ $javalin.state.Organizations }}</li>
			<li><b>Workspaces:</b> {{ $javalin.state.Workspaces }}; <b>Courses:</b> {{ $javalin.state.Courses }}; <b>Lessons:</b> {{ $javalin.state.Lessons }}</li>
		</ul>

		<h3>Server</h3>
		<ul>
			<li><b>CPUs Available:</b> {{ $javalin.state.CPUs }}</li>
			<li><b>Total Disk Space:</b> {{ $javalin.state.DiskTotalSpace / 1E9 }} GB</li>
			<li><b>Disk Space Free:</b> {{ $javalin.state.DiskSpaceFree / 1E9 }} GB</li>
			<li title="RAM allocated to the server; may be less than your total RAM"><b>RAM Allocated:</b> {{ $javalin.state.RAMTotal / 1E6 }} MB</li>
			<li><b>RAM Used:</b> {{ $javalin.state.RAMUsage / 1E6 }} MB</li>
		</ul>
	</div>
</template>

<script>
	Vue.component("dashboard", {
		template: "#dashboard"
	});
</script>
