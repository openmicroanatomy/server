<template id="setup">
	<form>
		<input type="text" placeholder="Organization name" />

		<hr />

		<input type="email" placeholder="Email" />
		<input type="text"  placeholder="Name" />
		<input type="password" placeholder="Password" />
		<input type="password" placeholder="Repeat password" />

		<input type="button" value="Submit" />
	</form>
</template>

<script>
	Vue.component("setup", {
		template: "#setup"
	});
</script>
